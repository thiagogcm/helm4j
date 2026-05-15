#!/usr/bin/env bun

import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

import {
  ensureRequiredBins,
  exitCode,
  lines,
  run,
  step,
  text,
} from "./lib/process";
import {
  kindNetworkIpv4Subnet,
  metalLbRangeFromSubnet,
  type MetalLbRange,
} from "./lib/metallb";

const WORKSPACE_ROOT = resolve(dirname(fileURLToPath(import.meta.url)), "..");

const CLUSTER_NAME = "helm4j";
const KIND_NODE_IMAGE =
  "kindest/node:v1.35.1@sha256:05d7bcdefbda08b4e038f644c4df690cdac3fba8b06f8289f30e10026720a1ab";
const METALLB_VERSION = "v0.15.3";
const ENVOY_IMAGE = "envoyproxy/envoy:v1.38.0";
const WSL_PROXY_CONTAINER = "wsl2proxy";
const REQUIRED_BIN = ["kubectl", "kind", "helm"];
const WSL_PROXY_PORTS = [80, 443, 4317];

async function isWsl2(): Promise<boolean> {
  try {
    return /WSL2/i.test(await Bun.file("/proc/version").text());
  } catch {
    return false;
  }
}

async function ensureWsl2Prereqs(): Promise<void> {
  if (Bun.which("lsof") === null) {
    console.error(
      "lsof is required to run the WSL2 proxy checks. Please make sure it is installed and available in your PATH.",
    );
    process.exit(1);
  }

  const unprivPortStart = (
    await Bun.file("/proc/sys/net/ipv4/ip_unprivileged_port_start").text()
  ).trim();

  if (Number.parseInt(unprivPortStart, 10) !== 80) {
    console.error(
      "Port 80 is not enabled. Please make sure it is available before running this script.",
    );
    console.error(
      "You can enable it by running the following command: echo 80 | sudo tee /proc/sys/net/ipv4/ip_unprivileged_port_start",
    );
    process.exit(1);
  }
}

async function ensurePortsAvailable(ports: readonly number[]): Promise<void> {
  for (const port of ports) {
    // -sTCP:LISTEN restricts the match to local listening sockets;
    // without it, lsof matches outbound connections too (an HTTPS
    // request from this process to api.anthropic.com:443 would
    // false-positive on `:443`).
    if ((await exitCode(["lsof", "-iTCP", `:${port}`, "-sTCP:LISTEN"])) === 0) {
      console.error(
        `Port ${port} is already in use. Please make sure it is available before running this script.`,
      );
      process.exit(1);
    }
  }
}

async function handleExistingCluster(): Promise<void> {
  const clusters = lines(await text(["kind", "get", "clusters"]));

  if (!clusters.includes(CLUSTER_NAME)) {
    return;
  }

  const deleteCluster =
    prompt(`Cluster ${CLUSTER_NAME} already exists. Do you want to delete it? [y/N] `) ??
    "";

  if (deleteCluster.trim().toLowerCase() === "y") {
    await run(["kind", "delete", "cluster", "--name", CLUSTER_NAME]);
    return;
  }

  console.log("Exiting...");
  process.exit(0);
}

function renderKindConfig(wsl2: boolean): string {
  // ClusterTrustBundle + ClusterTrustBundleProjection are needed for the
  // dev-root-ca CTB projection that every workload mounts under /trust.
  // Use kind's TOP-LEVEL `featureGates` and `runtimeConfig` fields rather
  // than kubeadmConfigPatches — kind's JSON-merge of the patches replaces
  // its own apiServer/kubelet defaults with whatever shape is patched in,
  // which has historically silently dropped these flags. The top-level
  // fields wire feature gates into apiserver, kubelet AND etcd consistently.
  let config = `kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
featureGates:
  ClusterTrustBundle: true
  ClusterTrustBundleProjection: true
runtimeConfig:
  "certificates.k8s.io/v1beta1": "true"
networking:
  ipFamily: dual
  apiServerAddress: "127.0.0.1"
nodes:
  - role: control-plane
    image: ${KIND_NODE_IMAGE}
    extraMounts:
      - hostPath: ${WORKSPACE_ROOT}
        containerPath: /workspace
        readOnly: false
    kubeadmConfigPatches:
      - |
        apiVersion: kubeadm.k8s.io/v1beta4
        kind: InitConfiguration
        metadata:
          name: init-config
        nodeRegistration:
          kubeletExtraArgs:
            - name: node-labels
              value: "ingress-ready=true"`;

  if (!wsl2) {
    config += `
    extraPortMappings:
      - containerPort: 80
        hostPort: 80
        protocol: TCP
      - containerPort: 4317
        hostPort: 4317
        protocol: TCP
      - containerPort: 443
        hostPort: 443
        protocol: TCP`;
  }

  return `${config}\n`;
}

function renderIpAddressPoolConfig(range: MetalLbRange): string {
  return `apiVersion: metallb.io/v1beta1
kind: IPAddressPool
metadata:
  namespace: metallb-system
  name: config
spec:
  addresses:
    - ${range.firstIp}-${range.lastIp}
`;
}

function renderL2AdvertisementConfig(): string {
  return `apiVersion: metallb.io/v1beta1
kind: L2Advertisement
metadata:
  namespace: metallb-system
  name: l2config
spec:
  ipAddressPools:
    - config
`;
}

export function renderWsl2ProxyConfig([firstIp]: [string]): string {
  return `static_resources:
  listeners:
    - name: listener_0
      address:
        socket_address:
          address: 0.0.0.0
          port_value: 80
      filter_chains:
        - filters:
            - name: envoy.filters.network.http_connection_manager
              typed_config:
                "@type": type.googleapis.com/envoy.extensions.filters.network.http_connection_manager.v3.HttpConnectionManager
                codec_type: AUTO
                stat_prefix: ingress_http
                upgrade_configs:
                  - upgrade_type: websocket
                access_log:
                  - name: envoy.access_loggers.stdout
                    typed_config:
                      "@type": type.googleapis.com/envoy.extensions.access_loggers.stream.v3.StdoutAccessLog
                      log_format:
                        text_format_source:
                          inline_string: "[%START_TIME%] %REQ(:METHOD)% %REQ(X-ENVOY-ORIGINAL-PATH?:PATH)% %RESPONSE_CODE%\\n"
                route_config:
                  name: local_route
                  virtual_hosts:
                    - name: local_service
                      domains:
                        - "*"
                      routes:
                        - match:
                            prefix: "/"
                          route:
                            cluster: service_backend
                http_filters:
                  - name: envoy.filters.http.router
                    typed_config:
                      "@type": type.googleapis.com/envoy.extensions.filters.http.router.v3.Router
    - name: listener_grpc
      address:
        socket_address:
          address: 0.0.0.0
          port_value: 4317
      filter_chains:
        - filters:
            - name: envoy.filters.network.http_connection_manager
              typed_config:
                "@type": type.googleapis.com/envoy.extensions.filters.network.http_connection_manager.v3.HttpConnectionManager
                codec_type: AUTO
                stat_prefix: ingress_grpc
                http2_protocol_options: {}
                access_log:
                  - name: envoy.access_loggers.stdout
                    typed_config:
                      "@type": type.googleapis.com/envoy.extensions.access_loggers.stream.v3.StdoutAccessLog
                      log_format:
                        text_format_source:
                          inline_string: "[%START_TIME%] gRPC %REQ(:METHOD)% %REQ(X-ENVOY-ORIGINAL-PATH?:PATH)% %RESPONSE_CODE%\\n"
                route_config:
                  name: grpc_route
                  virtual_hosts:
                    - name: grpc_service
                      domains:
                        - "*"
                      routes:
                        - match:
                            prefix: "/"
                          route:
                            cluster: service_backend_grpc
                http_filters:
                  - name: envoy.filters.http.router
                    typed_config:
                      "@type": type.googleapis.com/envoy.extensions.filters.http.router.v3.Router
    - name: listener_tls
      address:
        socket_address:
          address: 0.0.0.0
          port_value: 443
      filter_chains:
        - filters:
            - name: envoy.filters.network.tcp_proxy
              typed_config:
                "@type": type.googleapis.com/envoy.extensions.filters.network.tcp_proxy.v3.TcpProxy
                stat_prefix: tls_passthrough
                cluster: service_backend_tls
                access_log:
                  - name: envoy.access_loggers.stdout
                    typed_config:
                      "@type": type.googleapis.com/envoy.extensions.access_loggers.stream.v3.StdoutAccessLog
                      log_format:
                        text_format_source:
                          inline_string: "[%START_TIME%] TLS passthrough %RESPONSE_CODE%\\n"
  clusters:
    - name: service_backend
      connect_timeout: 0.25s
      type: STATIC
      lb_policy: ROUND_ROBIN
      load_assignment:
        cluster_name: service_backend
        endpoints:
          - lb_endpoints:
              - endpoint:
                  address:
                    socket_address:
                      address: ${firstIp}
                      port_value: 80
    - name: service_backend_grpc
      connect_timeout: 0.25s
      type: STATIC
      lb_policy: ROUND_ROBIN
      typed_extension_protocol_options:
        envoy.extensions.upstreams.http.v3.HttpProtocolOptions:
          "@type": type.googleapis.com/envoy.extensions.upstreams.http.v3.HttpProtocolOptions
          explicit_http_config:
            http2_protocol_options: {}
      load_assignment:
        cluster_name: service_backend_grpc
        endpoints:
          - lb_endpoints:
              - endpoint:
                  address:
                    socket_address:
                      address: ${firstIp}
                      port_value: 4317
    - name: service_backend_tls
      connect_timeout: 0.25s
      type: STATIC
      lb_policy: ROUND_ROBIN
      load_assignment:
        cluster_name: service_backend_tls
        endpoints:
          - lb_endpoints:
              - endpoint:
                  address:
                    socket_address:
                      address: ${firstIp}
                      port_value: 443

overload_manager:
  resource_monitors:
    - name: "envoy.resource_monitors.global_downstream_max_connections"
      typed_config:
        "@type": type.googleapis.com/envoy.extensions.resource_monitors.downstream_connections.v3.DownstreamConnectionsConfig
        max_active_downstream_connections: 10000

admin:
  access_log:
    - name: envoy.access_loggers.stdout
      typed_config:
        "@type": type.googleapis.com/envoy.extensions.access_loggers.stream.v3.StdoutAccessLog
  address:
    socket_address:
      address: 127.0.0.1
      port_value: 9901
`;
}

async function containerExists(name: string): Promise<boolean> {
  return lines(await text(["docker", "ps", "-a", "--format", "{{.Names}}"])).includes(
    name,
  );
}

async function startWsl2Proxy(range: MetalLbRange): Promise<void> {
  const config = renderWsl2ProxyConfig(range.proxyIps);

  await step("Validating WSL2 Envoy proxy config", () =>
    run([
      "docker",
      "run",
      "--rm",
      ENVOY_IMAGE,
      "--mode",
      "validate",
      "--config-yaml",
      config,
    ]),
  );

  if (await containerExists(WSL_PROXY_CONTAINER)) {
    await step(`Removing existing ${WSL_PROXY_CONTAINER} container`, () =>
      run(["docker", "rm", "-f", WSL_PROXY_CONTAINER]),
    );
  }

  await ensurePortsAvailable(WSL_PROXY_PORTS);

  await run([
    "docker",
    "run",
    "-d",
    "--rm",
    "--name",
    WSL_PROXY_CONTAINER,
    "--network",
    "host",
    ENVOY_IMAGE,
    "--config-yaml",
    config,
  ]);
}

async function main(): Promise<void> {
  await ensureRequiredBins(REQUIRED_BIN);

  const wsl2 = await isWsl2();
  if (wsl2) {
    await ensureWsl2Prereqs();
  }

  await handleExistingCluster();

  await step("Creating Kind cluster", () =>
    run(
      ["kind", "create", "cluster", "--name", CLUSTER_NAME, "--config", "-"],
      { stdin: renderKindConfig(wsl2) },
    ),
  );

  const range = metalLbRangeFromSubnet(await kindNetworkIpv4Subnet());

  await run([
    "kubectl",
    "apply",
    "-f",
    `https://raw.githubusercontent.com/metallb/metallb/${METALLB_VERSION}/config/manifests/metallb-native.yaml`,
  ]);

  await step("Waiting MetalLB controller", () =>
    run([
      "kubectl",
      "-n",
      "metallb-system",
      "rollout",
      "status",
      "deployment",
      "controller",
    ]),
  );

  await run(["kubectl", "apply", "-f", "-"], {
    stdin: renderIpAddressPoolConfig(range),
  });
  await run(["kubectl", "apply", "-f", "-"], {
    stdin: renderL2AdvertisementConfig(),
  });

  if (wsl2) {
    await startWsl2Proxy(range);
  }

  await step("Waiting MetalLB speaker", () =>
    run([
      "kubectl",
      "-n",
      "metallb-system",
      "rollout",
      "status",
      "daemonset",
      "speaker",
    ]),
  );
}

if (import.meta.main) {
  await main();
}

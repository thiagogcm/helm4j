// MetalLB IP range derivation from the Kind docker network subnet.
// Shared between scripts/dev-env.ts (cluster bootstrap) and
// kubernetes/scripts/bootstrap.ts (Gateway LB IP pinning).

import { text } from "./process";

type DockerNetwork = {
  IPAM?: {
    Config?: Array<{
      Subnet?: string;
    }>;
  };
};

export type MetalLbRange = {
  firstIp: string;
  lastIp: string;
  // Primary VIP only. Earlier iterations of dev-env.ts used a pair so the
  // WSL2 host envoy could round-robin two endpoints, but MetalLB rejects
  // multi-IP allocation within the same address family on a single Service.
  proxyIps: [string];
};

export async function kindNetworkIpv4Subnet(): Promise<string> {
  const networks = JSON.parse(await text(["docker", "network", "inspect", "kind"])) as
    | DockerNetwork[]
    | unknown;

  if (!Array.isArray(networks)) {
    throw new Error("Unexpected docker network inspect response for network kind.");
  }

  for (const network of networks) {
    for (const ipamConfig of network.IPAM?.Config ?? []) {
      if (ipamConfig.Subnet?.match(/^\d+\.\d+\.\d+\.\d+\/\d+$/)) {
        return ipamConfig.Subnet;
      }
    }
  }

  throw new Error("Could not find an IPv4 subnet for Docker network kind.");
}

export function metalLbRangeFromSubnet(subnet: string): MetalLbRange {
  const match = subnet.match(/^(\d+)\.(\d+)\.\d+\.\d+\/\d+$/);

  if (match === null) {
    throw new Error(`Unsupported IPv4 subnet format for MetalLB range: ${subnet}`);
  }

  const prefix = `${match[1]}.${match[2]}`;
  const firstIp = `${prefix}.255.70`;
  const lastIp = `${prefix}.255.84`;

  return {
    firstIp,
    lastIp,
    proxyIps: [firstIp],
  };
}

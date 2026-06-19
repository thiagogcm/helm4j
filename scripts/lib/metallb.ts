// MetalLB IP range derivation from the Kind docker network subnet.

import { $ } from "bun";

type DockerNetwork = {
  IPAM?: {
    Config?: Array<{ Subnet?: string }>;
  };
};

export type MetalLbRange = {
  firstIp: string;
  lastIp: string;
};

const IPV4_CIDR = /^(\d+)\.(\d+)\.\d+\.\d+\/\d+$/;

export async function kindNetworkIpv4Subnet(): Promise<string> {
  const networks = (await $`docker network inspect kind`.json()) as DockerNetwork[];

  const subnet = networks
    .flatMap((network) => network.IPAM?.Config ?? [])
    .map((config) => config.Subnet)
    .find((candidate) => candidate !== undefined && IPV4_CIDR.test(candidate));

  if (subnet === undefined) {
    throw new Error("Could not find an IPv4 subnet for Docker network kind.");
  }

  return subnet;
}

export function metalLbRangeFromSubnet(subnet: string): MetalLbRange {
  const match = subnet.match(IPV4_CIDR);

  if (match === null) {
    throw new Error(`Unsupported IPv4 subnet format for MetalLB range: ${subnet}`);
  }

  // firstIp..lastIp is the pool MetalLB hands out; the Gateway Service lands on
  // firstIp, the single VIP the WSL2 host envoy targets (MetalLB won't allocate
  // multiple same-family IPs to one Service, so there is no second endpoint).
  const prefix = `${match[1]}.${match[2]}`;
  return {
    firstIp: `${prefix}.255.70`,
    lastIp: `${prefix}.255.84`,
  };
}

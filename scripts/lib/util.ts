// CLI helpers for the workspace Bun scripts: output parsing, progress logging,
// and PATH preflight checks. Subprocess execution itself lives in Bun Shell (`$`).

export function lines(value: string): string[] {
  return value
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean);
}

export async function step<T>(label: string, action: () => Promise<T>): Promise<T> {
  console.log(`${label}...`);
  return action();
}

export function ensureBins(bins: readonly string[]): void {
  const missing = bins.filter((bin) => Bun.which(bin) === null);
  if (missing.length === 0) {
    return;
  }

  const plural = missing.length > 1;
  console.error(
    `Missing required ${plural ? "binaries" : "binary"}: ${missing.join(", ")}. ` +
      `Please install ${plural ? "them" : "it"} and ensure ${plural ? "they are" : "it is"} on your PATH.`,
  );
  process.exit(1);
}

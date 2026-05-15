// Shared subprocess helpers for workspace-level Bun scripts. Extracted from
// scripts/dev-env.ts so build-images.ts and kubernetes/scripts/bootstrap.ts
// can reuse the same conventions (stdio handling, allowFailure, step logging).

export type StdioMode = "pipe" | "inherit" | "ignore";

export type CommandOptions = {
  stdin?: string;
  stdout?: StdioMode;
  stderr?: StdioMode;
  allowFailure?: boolean;
  cwd?: string;
};

export type CommandResult = {
  exitCode: number;
  stdout: string;
};

export function formatCommand(command: readonly string[]): string {
  return command
    .map((arg) => (/[\s"'$`\\]/.test(arg) ? JSON.stringify(arg) : arg))
    .join(" ");
}

export async function command(
  argv: string[],
  options: CommandOptions = {},
): Promise<CommandResult> {
  const stdout = options.stdout ?? "inherit";
  const proc = Bun.spawn(argv, {
    cwd: options.cwd,
    stdin: options.stdin === undefined ? "inherit" : "pipe",
    stdout,
    stderr: options.stderr ?? "inherit",
  });

  if (options.stdin !== undefined) {
    proc.stdin.write(options.stdin);
    proc.stdin.end();
  }

  const stdoutPromise =
    stdout === "pipe" ? new Response(proc.stdout).text() : Promise.resolve("");
  const [stdoutText, exitCode] = await Promise.all([
    stdoutPromise,
    proc.exited,
  ]);

  if (exitCode !== 0 && !options.allowFailure) {
    throw new Error(
      `Command failed with exit code ${exitCode}: ${formatCommand(argv)}`,
    );
  }

  return { exitCode, stdout: stdoutText };
}

export async function run(argv: string[], options: CommandOptions = {}): Promise<void> {
  await command(argv, options);
}

export async function text(argv: string[], options: CommandOptions = {}): Promise<string> {
  return (await command(argv, { ...options, stdout: "pipe" })).stdout;
}

export async function exitCode(argv: string[]): Promise<number> {
  return (
    await command(argv, {
      stdout: "ignore",
      stderr: "ignore",
      allowFailure: true,
    })
  ).exitCode;
}

export function lines(value: string): string[] {
  return value
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean);
}

export async function step(label: string, action: () => Promise<void>): Promise<void> {
  console.log(`${label}...`);
  await action();
}

export async function ensureRequiredBins(bins: readonly string[]): Promise<void> {
  for (const bin of bins) {
    if (Bun.which(bin) === null) {
      console.error(
        `${bin} is required to run this script. Please make sure it is installed and available in your PATH.`,
      );
      process.exit(1);
    }
  }
}

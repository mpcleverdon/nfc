export interface NfcPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
}

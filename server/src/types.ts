export type BridgeTask =
  | { id: string; type: "search"; query: string }
  | { id: string; type: "fetch"; url: string };

export interface PageExtract {
  url: string;
  title: string;
  text: string;
}

export interface BridgeResponse {
  id: string;
  ok: boolean;
  data?: PageExtract;
  error?: string;
}

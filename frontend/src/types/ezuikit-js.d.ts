declare module 'ezuikit-js' {
  export interface EZUIKitPlayerOptions {
    id: string;
    accessToken: string;
    url: string;
    width: number | string;
    height: number | string;
    staticPath?: string;
    template?: string;
    audio?: boolean;
    autoPlay?: boolean;
    scaleMode?: number;
    language?: string;
    streamInfoCBType?: 0 | 1;
    handleSuccess?: () => void;
    handleError?: (error: unknown) => void;
  }

  export interface EZUIKitEventEmitter {
    on(event: string, handler: (data?: any) => void): void;
    off(event: string, handler: (data?: any) => void): void;
  }

  export class EZUIKitPlayer {
    static EVENTS: Record<string, string>;

    eventEmitter: EZUIKitEventEmitter;

    constructor(options: EZUIKitPlayerOptions);

    play(): Promise<void> | void;
    stop(): Promise<void> | void;
    resize(width: number, height: number): void;
    destroy(): Promise<void> | void;
  }
}

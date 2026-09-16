// Compose Wasm / sqlite-wasm 需要 SharedArrayBuffer，浏览器要求 COOP/COEP
;(function (config) {
    config.devServer = config.devServer || {};
    config.devServer.headers = [
        { key: 'Cross-Origin-Opener-Policy', value: 'same-origin' },
        { key: 'Cross-Origin-Embedder-Policy', value: 'require-corp' }
    ];
})(config);

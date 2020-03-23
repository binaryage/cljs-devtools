const puppeteer = require('puppeteer');

const path = process.argv[2];
const page = process.argv[3];

const httpServer = require('http-server');

const host = '127.0.0.1';
const port = 3000;

const url = `http://${host}:${port}/${page}`;

const serverOptions = {root: path};

const server = httpServer.createServer(serverOptions);

// see https://github.com/puppeteer/puppeteer/issues/3397#issuecomment-434970058
async function logMsg(msg) {
  // serialize my args the way I want
  const args = await Promise.all(msg.args().map(arg => arg.executionContext().evaluate(arg => {
    // I'm in a page context now. If my arg is an error - get me its message.
    if (arg instanceof Error)
      return arg.message;
    // return arg right away. since we use `executionContext.evaluate`, it'll return JSON value of
    // the argument if possible, or `undefined` if it fails to stringify it.
    return arg;
  }, arg)));
  console.log(...args);
}

server.listen(port, host, function () {
  console.log(`[puppeteer] Server running at http://${host}:${port}/`);

  (async () => {
    const browser = await puppeteer.launch();
    const page = await browser.newPage();
    const version = await page.browser().version();
    console.log("[puppeteer] Chrome version:", version);
    page.on('console', logMsg);
    console.log("[puppeteer] Navigating to: ", url);
    await page.goto(url);

    const failures = await page.evaluate(() => {
      return window["test-failures"];
    });

    await browser.close();

    process.exit(failures ? 100 : 0);
  })();

});

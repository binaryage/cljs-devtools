const puppeteer = require('puppeteer');

const path = process.argv[2];
const page = process.argv[3];

const httpServer = require('http-server');

const host = '127.0.0.1';
const port = 3000;

const url = `http://${host}:${port}/${page}`;

const serverOptions = {root: path};

const server = httpServer.createServer(serverOptions);

server.listen(port, host, function () {
  console.log(`Server running at http://${host}:${port}/`);

  (async () => {
    const browser = await puppeteer.launch();
    const page = await browser.newPage();
    page.on('console', msg => console.log(msg.text()));
    console.log("navigating to: ", url);
    await page.goto(url);

    const failures = await page.evaluate(() => {
      return window["test-failures"];
    });

    await browser.close();

    process.exit(failures ? 100 : 0);
  })();

});

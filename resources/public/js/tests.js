// use goog.dependencies_ to list all relevant task namespaces
// we rely on the fact that we are running unoptimized clojurescript code here
// so all namespaces are present with original names

function pathToNamespace(path) {
    return path.replace(/_/g, "-").replace(/\//g, ".");
}

function getIndex(re) {
    const index = [];
    if (goog.DEPENDENCIES_ENABLED) {
        //noinspection JSAccessibilityCheck
        const container = goog.dependencies_.requires;
        for (const item in container) {
            if (container.hasOwnProperty(item)) {
                const m = item.match(re);
                if (m) {
                    const path = m[1];
                    index.push(pathToNamespace(path));
                }
            }
        }
    }
    return index;
}

function genTaskList(runnerUrl, tasks) {
    const lines = [
        "<div class='tasks'>",
        "<span class='tasks-title'>AVAILABLE TESTS:</span>",
        "<ol class='suite-list'>"];

    for (let i = 0; i < tasks.length; i++) {
        const ns = tasks[i];
        const line = "<li><a href=\"" + runnerUrl + "?ns=" + ns + "\">" + ns + "</a></li>";
        lines.push(line);
    }

    lines.push("</ol>");
    lines.push("</div>");
    return lines.join("\n");
}

function genTests(baseUrl) {
    const tasks = getIndex(/.*?devtools_sample\/tests\/(.*)\.js/);
    return genTaskList(baseUrl || "", tasks.sort());
}

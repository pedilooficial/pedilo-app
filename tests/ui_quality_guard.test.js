const assert = require("node:assert/strict");
const {test} = require("node:test");
const {spawnSync} = require("node:child_process");

test("ui quality guard passes", () => {
  const result = spawnSync("bash", ["tools/guards/check_ui_quality.sh"], {
    encoding: "utf8",
  });
  assert.equal(result.status, 0, result.stderr || result.stdout);
});

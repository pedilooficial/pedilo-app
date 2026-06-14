const assert = require("node:assert/strict");
const {test} = require("node:test");
const {spawnSync} = require("node:child_process");

test("app and backend source stay free of removed public identity terms", () => {
  const forbidden = [
    "UserRole" + ".Customer",
    "cust" + "omerId",
    "signIn" + "Anonymously",
    "id" + "Token",
    "isOwner",
  ];
  const paths = ["app/src/main", "firestore.rules"];

  for (const term of forbidden) {
    const result = spawnSync("rg", ["-n", "--fixed-strings", term, ...paths], {
      encoding: "utf8",
    });
    assert.notEqual(result.status, 0, result.stdout);
  }
});

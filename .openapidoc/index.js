const fs = require("fs");
const yaml = require("js-yaml");
const fetch = require("node-fetch");
const ghpages = require("gh-pages");

const config = new (function () {
  this.repo = "git@github.com:PegaSysEng/eth2signer.git";
  this.branch = "gh-pages";
  this.specPath = "../core/build/resources/main/openapi/eth2signer.yaml";
  this.specVersion = "";
  this.versionsFileName = "versions.json";
  this.versionsFileUrl = `https://github.com/PegaSysEng/eth2signer/raw/${this.branch}/${this.versionsFileName}`;
  this.distDir = "./dist";
  this.versionsFileDist = `${this.distDir}/${this.versionsFileName}`;
})();

/**
 * Main function to prepare and publish openapi spec to gh-pages branch
 */
async function main() {
  prepareDistDir();
  calculateVersionFromSpec();
  copySpecFileToDist();
  await updateVersionJson();
  cleanGhPagesCache();
  await publishToGHPages();
}

/**
 * Re-create dist dir
 */
function prepareDistDir() {
  fs.rmdirSync(config.distDir, { recursive: true });
  fs.mkdirSync(config.distDir, { recursive: true });
}

/**
 * Parse OpenApi yaml and update config spec version.
 */
function calculateVersionFromSpec() {
  const data = yaml.safeLoad(fs.readFileSync(config.specPath, "utf8"));
  config.specVersion = data.info.version;
}

/**
 * Determine if its a release version from spec version. It contains -dev- as a result from our gradle build
 */
function isReleaseVersion() {
  return !config.specVersion.includes("-dev-");
}

/**
 * Fetch versions.json
 */
async function fetchVersions() {
  const response = await fetch(config.versionsFileUrl);
  if (response.ok) {
    return response.json();
  } else {
    throw new Error("Versions.json fetch Status: " + response.statusText);
  }
}

/**
 * update versions.json
 * @param versionsJson
 */
async function updateVersionJson() {
  if (!isReleaseVersion()) {
    return;
  }

  const versionsJson = await fetchVersions();

  versionsJson[config.specVersion] = {
    spec: config.specVersion,
    source: config.specVersion,
  };
  versionsJson["stable"] = {
    spec: config.specVersion,
    source: config.specVersion,
  };
  fs.writeFileSync(
    config.versionsFileDist,
    JSON.stringify(versionsJson, null, 1)
  );
}

function ghPagesConfig() {
  return {
    add: true,
    branch: config.branch,
    repo: config.repo,
    user: {
      name: process.env["CIRCLE_USERNAME"] ? process.env["CIRCLE_USERNAME"] :  "CircleCI Build",
      email: process.env["CIRCLE_USERNAME"] ? `${process.env["CIRCLE_USERNAME"]}@users.noreply.github.com` : "ci-build@consensys.net",
    },
    message: `[skip ci] OpenAPI Publish ${config.specVersion}`,
  };
}

function cleanGhPagesCache() {
  ghpages.clean();
}

/**
 * Publish dist folder to gh-pages branch
 */
async function publishToGHPages() {
  return new Promise((resolve, reject) => {
    ghpages.publish(config.distDir, ghPagesConfig(), (err) => {
      if (err) {
        reject(err);
      }
      resolve();
    });
  });
}

function copySpecFileToDist() {
  fs.copyFileSync(config.specPath, `${config.distDir}/eth2signer-latest.yaml`);
  if (isReleaseVersion()) {
    fs.copyFileSync(
      config.specPath,
      `${config.distDir}/eth2signer-${config.specVersion}.yaml`
    );
  }
}

// start execution of main method
main()
  .then(() => {
    process.stdout.write(
      `OpenAPI specs [${config.specVersion}] published to [${config.branch}] using user [${ghPagesConfig().user.name}].\n`
    );
  })
  .catch((err) => {
    process.stderr.write(
      `OpenAPI spec [${config.specVersion}] failed to publish to [${config.branch}] using user [${ghPagesConfig().user.name}]: ${err.message}\n`,
      () => process.exit(1)
    );
  });

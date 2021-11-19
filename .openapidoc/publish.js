const fs = require("fs");
const fetch = require("node-fetch");
const ghpages = require("gh-pages");
const tree = require('tree-node-cli');

const config = require("./config.js");

const log = (...args) => console.log(...args); // eslint-disable-line no-console

/**
 * Main function to prepare and publish openapi spec to gh-pages branch
 */
async function main() {
  const cfg = config.getConfig();
  const { distDir, specDir, versionDetails, versionJsonDetails, ghPagesConfig } = cfg;
  try {
    log(`Starting publishing OpenApi spec release: ${versionDetails.version}`);

    // step1: clean distDir
    prepareDistDir(distDir);

    // step2: Copy directory core/build/publish to distDir/latest
    log(`Copying ${specDir} to ${distDir}/latest`);
    fs.cpSync(specDir, distDir + "/latest", {recursive: true});

    // step 3: if non-dev version,
    //  then copy 'latest' dir to '$version' dir
    //  and fetch and update versions.json

    if (versionDetails.isReleaseVersion) {
      log("Stable version detected.")
      log(`Copying ${specDir} to ${distDir}/${versionDetails.version}`);
      fs.cpSync(specDir, distDir + "/" + versionDetails.version, {recursive: true});

      log("Fetching " + versionJsonDetails.url);
      const versionsJson = await fetchVersions(versionJsonDetails.url);

      log("Adding stable and current version entry into versions.json");
      const updatedVersionsJson = updateVersions(versionsJson, versionDetails.version);

      log("Saving versions.json to " + versionJsonDetails.dist);
      saveVersionsJson(updatedVersionsJson, versionJsonDetails.dist);
    }

    log("Publishing following files: ");
    log(tree(distDir));

    // step 4: Publish/commit to gh-pages branch
    cleanGhPagesCache();
    await publishToGHPages(distDir, ghPagesConfig);
    log(
      `OpenAPI specs [${versionDetails.version}] published to [${ghPagesConfig.branch}] using user [${ghPagesConfig.user.name}]`
    );
  } catch (err) {
    log(`ERROR: OpenAPI spec failed to publish: ${err.message}`);
    log(config);
    process.exit(1);
  }
}

/**
 * Re-create dist dir
 * @param {string} dirPath
 */
function prepareDistDir(dirPath) {
  log("Cleaning up " + dirPath)
  fs.rmSync(dirPath, { recursive: true });
  fs.mkdirSync(dirPath, { recursive: true });
}

/**
 * Fetch versions.json
 */
async function fetchVersions(versionsUrl) {
  const response = await fetch(versionsUrl);
  if (response.ok) {
    const versionsJson = await response.json();
    return versionsJson;
  }

  throw new Error(
    `${versionsUrl} fetch failed with status: ${response.statusText}`
  );
}

/**
 * update versions by adding stable and version entry
 * @param versionsJson
 * @param {string} specVersion
 */
function updateVersions(versionsJson, specVersion) {
  versionsJson[specVersion] = {
    spec: specVersion,
    source: specVersion,
  };
  versionsJson["stable"] = {
    spec: specVersion,
    source: specVersion,
  };
  return versionsJson;
}

function saveVersionsJson(versionsJson, versionsDist) {
  fs.writeFileSync(versionsDist, JSON.stringify(versionsJson, null, 1));
}

function cleanGhPagesCache() {
  ghpages.clean();
}

/**
 * Publish dist folder to gh-pages branch
 */
async function publishToGHPages(distDir, ghPagesConfig) {
  return new Promise((resolve, reject) => {
    ghpages.publish(distDir, ghPagesConfig, (err) => {
      if (err) {
        reject(err);
      }
      resolve();
    });
  });
}

// start execution of main method
main();

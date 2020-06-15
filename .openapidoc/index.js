const fs = require("fs");
const yaml = require("js-yaml");
const fetch = require("node-fetch");
const ghpages = require("gh-pages");

// constants
const distDir = "./dist";
const repo = "git@github.com:PegaSysEng/eth2signer.git";
const branch = "gh-pages";
const spec = "../core/build/resources/main/openapi/eth2signer.yaml";
const versionsFileName = "versions.json";
const versionsFileUrl = `https://github.com/PegaSysEng/eth2signer/raw/${branch}/${versionsFileName}`;
const versionsFile = `${distDir}/${versionsFileName}`;

/**
 * Re-create dist dir
 */
function prepareDistDir() {
  fs.rmdirSync(distDir, { recursive: true });
  fs.mkdirSync(distDir, { recursive: true });
}

/**
 * Parse OpenApi yaml and return spec version.
 */
function calculateVersionFromSpec() {
  let data = yaml.safeLoad(fs.readFileSync(spec, "utf8"));
  return data.info.version;
}

/**
 * Determine if its a release version from spec version. It contains -dev- as a result from our gradle build
 * @param specVersion
 */
function isReleaseVersion(specVersion) {
  return !specVersion.includes("-dev-");
}

/**
 * update versions.json
 * @param specVersion
 * @param versionsJson
 */
async function updateVersionJson(specVersion, versionsJson) {
  versionsJson[specVersion] = { spec: specVersion, source: specVersion };
  versionsJson["stable"] = { spec: specVersion, source: specVersion };
  fs.writeFileSync(versionsFile, JSON.stringify(versionsJson, null, 1));
}

/**
 * Fetch versions.json
 */
async function fetchVersions() {
  console.log("Fetching " + versionsFileUrl);
  const versionJsonResponse = await fetch(versionsFileUrl);
  if (versionJsonResponse.ok) {
    return versionJsonResponse.json();
  } else {
    throw new Error(
      "Versions.json fetch Status: " + versionJsonResponse.statusText
    );
  }
}

/**
 * Publish dist folder to gh-pages branch
 * @param specVersion
 */
async function publishToGHPages(specVersion) {
  return new Promise((resolve, reject) => {
    ghpages.publish(
      distDir,
      {
        add: true,
        branch: branch,
        repo: repo,
        user: {
          name: "CircleCI Build",
          email: "ci-build@consensys.net",
        },
        message: `[skip ci] OpenAPI Publish ${specVersion}`,
      },
      (err) => {
        if (err) {
          reject(err);
        }
        resolve(true);
      }
    );
  });
}

function copySpecFileToDist(specVersion) {
  fs.copyFileSync(spec, `${distDir}/eth2signer-${specVersion}.yaml`);
}

/**
 * Main function to prepare and publish openapi spec to gh-pages branch
 */
async function main() {
  prepareDistDir();
  const specVersion = calculateVersionFromSpec();

  copySpecFileToDist("latest");

  if (isReleaseVersion(specVersion)) {
    copySpecFileToDist(specVersion);
    let versionsJson = await fetchVersions();
    await updateVersionJson(specVersion, versionsJson);
    console.log("Release versions updated");
  } else {
    console.log("Not a release version, bypassing updating versions.json");
  }

  await publishToGHPages(specVersion);
}

// start execution of main method
main()
  .then((result) => {
    console.log(`OpenAPI specs published to ${branch} successfully.`);
  })
  .catch((error) => {
    console.log(`OpenAPI spec publish failed to ${branch}`);
    console.log(error.message);
    process.exit(1);
  });

const fs = require("fs");
const yaml = require("js-yaml");
const { https } = require("follow-redirects");
const ghpages = require("gh-pages");

// constants
const repo = "git@github.com:PegaSysEng/eth2signer.git";
const branch = "gh-pages";
const spec = "../core/build/resources/main/openapi/eth2signer.yaml";
const versionsFileUrl =
  "https://github.com/PegaSysEng/eth2signer/raw/gh-pages/versions.json";
const distDir = "./dist";
const versionsFile = distDir + "/versions.json";

/**
 * Downloads the given source URL and return it as a string
 * @param source
 */
async function downloadAsString(source) {
  return new Promise((resolve, reject) => {
    let output = "";
    return https
      .get(source, (response) => {
        const { statusCode, headers } = response;
        if (statusCode >= 400)
          return reject(
            new Error(
              `Failed to download file "${source}". Status: ${statusCode}`
            )
          );

        response.on("data", (chunk) => (output += chunk));
        response.on("end", () => resolve(output));
      })
      .on("error", (err) => {
        return reject(err);
      });
  });
}

/**
 * Make distDir if it doesn't exist recursively
 */
function ensureDistDir() {
  fs.mkdirSync(distDir, { recursive: true });
}

/**
 * Clean and remove existing distDir
 */
function cleanDistDir() {
  fs.rmdirSync(distDir, { recursive: true });
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
 * Copy spec yaml to dist as as eth2signer-latest.yaml and optionally as eth2signer-specVer.yaml
 * @param specVersion
 */
function copySpecToDist(specVersion) {
  fs.copyFileSync(spec, `${distDir}/eth2signer-latest.yaml`);

  if (isReleaseVersion(specVersion)) {
    fs.copyFileSync(spec, `${distDir}/eth2signer-${specVersion}.yaml`);
  }
}

/**
 * Parse and update versions.json
 * @param specVersion
 * @param versionJsonString
 */
function updateVersionJson(specVersion, versionJsonString) {
  var versions = JSON.parse(versionJsonString);
  versions[specVersion] = { spec: specVersion, source: specVersion };
  versions["stable"] = { spec: specVersion, source: specVersion };
  fs.writeFileSync(versionsFile, JSON.stringify(versions, null, 1));
}

/**
 * Download versions.json for release version from gh-pages and then pass result to updateVersionJson
 * @param specVersion
 */
async function downloadAndUpdateVersionsFile(specVersion) {
  return new Promise((resolve, reject) => {
    if (!isReleaseVersion(specVersion)) {
      console.log("Not a release version, bypassing updating versions.json");
      return resolve(false);
    }

    return downloadAsString(versionsFileUrl)
      .then((value) => {
        try {
          updateVersionJson(specVersion, value);
          return resolve(true);
        } catch (err) {
          return reject(err);
        }
      })
      .catch((err) => {
        reject(err);
      });
  });
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

async function run() {
  cleanDistDir();
  ensureDistDir();

  let specVersion = calculateVersionFromSpec();
  console.log("Spec Version: " + specVersion);

  copySpecToDist(specVersion);
  await downloadAndUpdateVersionsFile(specVersion);
  await publishToGHPages(specVersion);

  console.log("OpenAPI specs published to gh-pages successfully.");
}

run().catch((error) => {
  console.log("OpenAPI spec publish failed");
  console.log(error.message);
  process.exit(1);
});

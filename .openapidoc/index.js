const fs = require("fs");
const yaml = require("js-yaml");
const { https } = require("follow-redirects");
const ghpages = require("gh-pages");

const repo = "git@github.com:PegaSysEng/eth2signer.git";
const branch = "gh-pages-test";
const spec = "../core/build/resources/main/openapi/eth2signer.yaml";
const versionsFileUrl =
  "https://github.com/PegaSysEng/eth2signer/raw/gh-pages/versions.json";
const distDir = "./dist/";
const versionsFile = distDir + "versions.json";

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

function ensureDistDir() {
  fs.mkdirSync(distDir, { recursive: true });
}

function cleanDistDir() {
  fs.rmdirSync(distDir, { recursive: true });
}

function calculateVersionFromSpec() {
  let data = yaml.safeLoad(fs.readFileSync(spec, "utf8"));
  return data.info.version;
}

function isReleaseVersion(specVersion) {
  return !specVersion.includes("-dev-");
}

function copySpecToDist(specVersion) {
  fs.copyFileSync(spec, distDir + "eth2signer-latest.yaml");

  if (isReleaseVersion(specVersion)) {
    fs.copyFileSync(spec, `${distDir}eth2signer-${specVersion}.yaml`);
  }
}

function updateVersionJson(specVersion, versionJsonString) {
  var versions = JSON.parse(versionJsonString);
  versions[specVersion] = { spec: specVersion, source: specVersion };
  versions["stable"] = { spec: specVersion, source: specVersion };
  fs.writeFileSync(versionsFile, JSON.stringify(versions, null, 1));
}

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

async function run() {
  cleanDistDir();
  ensureDistDir();

  let specVersion = calculateVersionFromSpec();
  console.log("Spec Version: " + specVersion);

  copySpecToDist(specVersion);
  await downloadAndUpdateVersionsFile(specVersion);

  await ghpages.publish(
    "./dist",
    {
      add: true,
      branch: branch,
      repo: repo,
      user: {
        name: "CircleCI Build",
        email: "ci-build@consensys.net",
      },
      message: `OpenAPI Publish ${specVersion}`,
    },
    (err) => {
      if (err) {
        console.log("OpenAPI spec publish failed");
        console.log("Error in gh-pages: " + err);
        process.exit(1);
      }
    }
  );

  console.log("Done");
}

run().catch((error) => {
  console.log("OpenAPI spec publish failed");
  console.log(error.message);
  process.exit(1);
});

const fs = require("fs");
const path = require("path");
const yaml = require("js-yaml");
const fetch = require("node-fetch");
const GitUrlParse = require("git-url-parse");
const ghpages = require("gh-pages");

const log = (...args) => console.log(...args); // eslint-disable-line no-console

function getConfig() {
  const repo = GitUrlParse(
    process.env.OA_GIT_URL || "git@github.com:PegaSysEng/web3signer.git"
  );
  const branch = process.env.OA_GH_PAGES_BRANCH || "gh-pages";
  const gitUserName = process.env.OA_GIT_USERNAME || "CircleCI Build";
  const gitUserEmail = process.env.OA_GIT_EMAIL || "ci-build@consensys.net";
  const specPath =
    process.env.OA_SPEC_PATH ||
    "../core/build/resources/main/openapi/web3signer.yaml";
  const specVersion = yaml.safeLoad(fs.readFileSync(specPath, "utf8")).info
    .version;
  const isReleaseVersion = !specVersion.includes("-dev-"); // gradle build puts -dev- for snapshot version
  const specFileNamePrefix = path.parse(specPath).name;
  const specFileNameExtension = path.extname(specPath);
  const versionsFileName = process.env.OA_VERSIONS_FILE_NAME || "versions.json";
  const versionsFileUrl = `https://${repo.source}/${repo.owner}/${repo.name}/raw/${branch}/${versionsFileName}`;
  const distDir = process.env.OA_DIST_DIR || "./dist";
  const versionsFileDist = path.join(distDir, versionsFileName);
  return {
    spec: {
      path: specPath,
      version: specVersion,
      isReleaseVersion: isReleaseVersion,
      latestDist: path.join(
        distDir,
        `${specFileNamePrefix}-latest${specFileNameExtension}`
      ),
      releaseDist: path.join(
        distDir,
        `${specFileNamePrefix}-${specVersion}${specFileNameExtension}`
      ),
    },
    distDir,
    versions: {
      url: versionsFileUrl,
      dist: versionsFileDist,
    },
    ghPagesConfig: {
      add: true, // allows gh-pages module to keep remote files
      branch: branch,
      repo: repo.href,
      user: {
        name: gitUserName,
        email: gitUserEmail,
      },
      message: `[skip ci] OpenAPI Publish ${specVersion}`,
    },
  };
}

/**
 * Main function to prepare and publish openapi spec to gh-pages branch
 */
async function main() {
  const config = getConfig();
  const { distDir, spec, versions, ghPagesConfig } = config;
  try {
    prepareDistDir(distDir);
    copySpecFileToDist(spec);

    if (spec.isReleaseVersion) {
      const versionsJson = await fetchVersions(versions.url);
      const updatedVersionsJson = updateVersions(versionsJson, spec.version);
      saveVersionsJson(updatedVersionsJson, versions.dist);
    }

    cleanGhPagesCache();
    await publishToGHPages(distDir, ghPagesConfig);
    log(
      `OpenAPI spec [${spec.version}] published to [${ghPagesConfig.branch}] using user [${ghPagesConfig.user.name}]`
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
  fs.rmdirSync(dirPath, { recursive: true });
  fs.mkdirSync(dirPath, { recursive: true });
}

function copySpecFileToDist(spec) {
  fs.copyFileSync(spec.path, spec.latestDist);
  if (spec.isReleaseVersion) {
    fs.copyFileSync(spec.path, spec.releaseDist);
  }
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
 * update versions
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

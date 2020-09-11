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

  const eth2SpecPath = "../core/build/resources/main/openapi/web3signer-eth2.yaml";
  const eth1SpecPath = "../core/build/resources/main/openapi/web3signer-eth1.yaml";

  // both spec files have same version
  const specVersion = yaml.safeLoad(fs.readFileSync(eth2SpecPath, "utf8")).info.version;
  const isReleaseVersion = !specVersion.includes("-dev-"); // gradle build puts -dev- for snapshot version

  const eth2SpecFileNamePrefix = path.parse(eth2SpecPath).name;
  const eth2SpecFileNameExtension = path.extname(eth2SpecPath);

  const eth1SpecFileNamePrefix = path.parse(eth1SpecPath).name;
  const eth1SpecFileNameExtension = path.extname(eth1SpecPath);

  const versionsFileName = process.env.OA_VERSIONS_FILE_NAME || "versions.json";
  const versionsFileUrl = `https://${repo.source}/${repo.owner}/${repo.name}/raw/${branch}/${versionsFileName}`;
  const distDir = process.env.OA_DIST_DIR || "./dist";
  const versionsFileDist = path.join(distDir, versionsFileName);
  return {
    eth2Spec: {
      path: eth2SpecPath,
      version: specVersion,
      isReleaseVersion: isReleaseVersion,
      latestDist: path.join(
        distDir,
        `${eth2SpecFileNamePrefix}-latest${eth2SpecFileNameExtension}`
      ),
      releaseDist: path.join(
        distDir,
        `${eth2SpecFileNamePrefix}-${specVersion}${eth2SpecFileNameExtension}`
      ),
    },
    eth1Spec: {
      path: eth1SpecPath,
      version: specVersion,
      isReleaseVersion: isReleaseVersion,
      latestDist: path.join(
        distDir,
        `${eth1SpecFileNamePrefix}-latest${eth1SpecFileNameExtension}`
      ),
      releaseDist: path.join(
        distDir,
        `${eth1SpecFileNamePrefix}-${specVersion}${eth1SpecFileNameExtension}`
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
  const { distDir, eth1Spec, eth2Spec, versions, ghPagesConfig } = config;
  try {
    prepareDistDir(distDir);
    copySpecFileToDist(eth2Spec);
    copySpecFileToDist(eth1Spec);

    if (eth2Spec.isReleaseVersion) {
      const versionsJson = await fetchVersions(versions.url);
      const updatedVersionsJson = updateVersions(versionsJson, eth2Spec.version);
      saveVersionsJson(updatedVersionsJson, versions.dist);
    }

    cleanGhPagesCache();
    await publishToGHPages(distDir, ghPagesConfig);
    log(
      `OpenAPI specs [${eth2Spec.version}] published to [${ghPagesConfig.branch}] using user [${ghPagesConfig.user.name}]`
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

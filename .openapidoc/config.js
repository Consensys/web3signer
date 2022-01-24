import fs from "fs";
import path from "path";
import yaml from "js-yaml";
import GitUrlParse from "git-url-parse";

// modify following using env variables in CI environment
const gitUrl =
  process.env.OA_GIT_URL || "git@github.com:ConsenSys/web3signer.git";
const gitUserName = process.env.OA_GIT_USERNAME || "CircleCI Build";
const gitEmail = process.env.OA_GIT_EMAIL || "ci-build@consensys.net";
const branch = process.env.OA_GH_PAGES_BRANCH || "gh-pages";

// modify following with caution
const distDir = process.env.OA_DIST_DIR || "./dist";
const specDir =
  process.env.OA_SPEC_DIR || "../core/build/resources/main/openapi-specs";
const specFile = specDir + "/eth2/web3signer.yaml";
const versionsFileName = process.env.OA_VERSIONS_FILE_NAME || "versions.json";

export default {
  getConfig,
};

function getConfig() {
  const repo = GitUrlParse(gitUrl);
  const versionDetails = calculateVersionDetails();
  const versionJsonDetails = calculateVersionJsonDetails(repo, branch);
  return {
    specDir: specDir,
    distDir: distDir,
    versionDetails: versionDetails,
    versionJsonDetails: versionJsonDetails,
    ghPagesConfig: {
      add: true, // allows gh-pages module to keep remote files
      branch: branch,
      repo: repo.href,
      user: {
        name: gitUserName,
        email: gitEmail,
      },
      message: `[skip ci] OpenAPI Publish [${versionDetails.version}]`,
    },
  };
}

function calculateVersionDetails() {
  const specVersion = readVersionFromSpecFile(specFile);
  const release = isReleaseVersion(specVersion);

  return {
    version: specVersion,
    isReleaseVersion: release,
  };
}

function readVersionFromSpecFile(specFile) {
  return yaml.load(fs.readFileSync(specFile, "utf8")).info.version;
}

function isReleaseVersion(specVersion) {
  // our main project's gradle's build calculateVersion adds "+<new commits since stable>-<hash>"
  // after the version for dev builds
  return !specVersion.includes("+");
}

function calculateVersionJsonDetails(repo, branch) {
  const versionsFileUrl = `https://${repo.source}/${repo.owner}/${repo.name}/raw/${branch}/${versionsFileName}`;
  const versionsFileDist = path.join(distDir, versionsFileName);
  return {
    url: versionsFileUrl,
    dist: versionsFileDist,
  };
}

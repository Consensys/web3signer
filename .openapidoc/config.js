const fs = require("fs");
const path = require("path");
const yaml = require("js-yaml");
const GitUrlParse = require("git-url-parse");

const distDir = process.env.OA_DIST_DIR || "./dist";
const specDir =
  process.env.OA_SPEC_DIR || "../core/build/resources/main/openapi";
const gitUrl =
  process.env.OA_GIT_URL || "git@github.com:ConsenSys/web3signer.git";
const gitUserName = process.env.OA_GIT_USERNAME || "CircleCI Build";
const gitEmail = process.env.OA_GIT_EMAIL || "ci-build@consensys.net";
const branch = process.env.OA_GH_PAGES_BRANCH || "gh-pages";
const versionsFileName = process.env.OA_VERSIONS_FILE_NAME || "versions.json";

module.exports = {
  getConfig,
};

function getConfig() {
  const repo = GitUrlParse(gitUrl);
  const specs = calculateSpecs();
  if (specs.length == 0) {
    throw new Error("Unable to parse specs in dist" + distDir);
  }

  return {
    specs: specs,
    distDir: distDir,
    versions: calculateVersionDetails(repo, branch),
    ghPagesConfig: {
      add: true, // allows gh-pages module to keep remote files
      branch: branch,
      repo: repo.href,
      user: {
        name: gitUserName,
        email: gitEmail,
      },
      message: `[skip ci] OpenAPI Publish [${specs[0].version}]`,
    },
  };
}

function calculateSpecs() {
  const extension = ".yaml";
  const specFiles = fs.readdirSync(specDir);
  var specArr = [];
  specFiles.forEach((file) => {
    if (path.extname(file).toLowerCase() === extension) {
      specArr.push(calculateSpecDetails(path.join(specDir, file)));
    }
  });
  return specArr;
}

function calculateSpecDetails(specFile) {
  const specVersion = calculateSpecVersion(specFile);
  const release = isReleaseVersion(specVersion);
  const latestDist = destinationPath(specFile, "latest");
  const releaseDist = destinationPath(specFile, specVersion);

  return {
    path: specFile,
    version: specVersion,
    isReleaseVersion: release,
    latestDist: latestDist,
    releaseDist: releaseDist,
  };
}

function calculateSpecVersion(specFile) {
  return yaml.safeLoad(fs.readFileSync(specFile, "utf8")).info.version;
}

function isReleaseVersion(specVersion) {
  // our main project's gradle's build calculateVersion puts -dev- for snapshot version
  return !specVersion.includes("-dev-");
}

function destinationPath(specFile, suffix) {
  const prefix = path.parse(specFile).name;
  const extension = path.extname(specFile);
  return path.join(distDir, `${prefix}-${suffix}${extension}`);
}

function calculateVersionDetails(repo, branch) {
  const versionsFileUrl = `https://${repo.source}/${repo.owner}/${repo.name}/raw/${branch}/${versionsFileName}`;
  const versionsFileDist = path.join(distDir, versionsFileName);
  return {
    url: versionsFileUrl,
    dist: versionsFileDist,
  };
}

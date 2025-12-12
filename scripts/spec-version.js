// Hardcoded versions for eth1 and eth2
const versions = {
  'eth2': { spec: 'openapi-specs/eth2', source: 'eth2' },
  'eth1': { spec: 'openapi-specs/eth1', source: 'eth1' }
};
const defaultVersion = 'eth2'; // Set eth2 as default

// Return the OpenAPI spec path for a version
function getSpecPath(version) {
  const spec = versions[version] ? versions[version].spec : versions[defaultVersion].spec;
  return `${spec}/web3signer.yaml`;
}

// Check if the version is the one displayed in the URL
function isDisplayedVersion(version, displayedVersion) {
  return displayedVersion === version || (displayedVersion === '' && version === defaultVersion);
}

// Update the dropdown to show only eth1 and eth2
function updateVersionsDropDown(versionFromUrl) {
  $("#versionsList").empty();
  $.each(versions, function(version, infos) {
    const item = $(`<a class="version-number-item dropdown-item" href="?version=${version}">${version}</a>`);
    if (isDisplayedVersion(version, versionFromUrl)) {
      item.addClass("active");
      $('#dropdownMenuButton').text(`${version}`);
      document.title = `Consensys Web3Signer ${version.toUpperCase()} API Documentation`;
    }
    item.appendTo("#versionsList");
  });
}

// Get the version from the URL (query or hash)
function getVersionFromURL() {
  const params = new URLSearchParams(window.location.search.substring(1));
  const version = params.get("version");
  return version || '';
}

// Initialize Redoc with the selected version
function updateSpec(version) {
  const element = $(`<redoc spec-url="${getSpecPath(version)}"></redoc>`);
  element.appendTo("main.container");
  Redoc.init(`${element.attr('spec-url')}?ts=${Date.now()}`, { hideHostname: true });
  updateVersionsDropDown(version);
}

// Initialize on page load
$(function() {
  updateSpec(getVersionFromURL());
});

// Update on hash change (e.g., manual URL update)
$(window).on('hashchange', function() {
  updateSpec(getVersionFromURL());
});
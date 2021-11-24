// default version list in case of JSON loading issue
let versions = {'latest':{'spec':'latest','source':'master'}};
let defaultVersion = Object.keys(versions)[0];

// return the json OPENAPI spec path to load for a version
// or a default version (first one in list) if no matching version.
function getSpecPath(version){
  let spec = (version in versions) ? versions[version].spec : versions[defaultVersion].spec;
  return `${spec}/${SPEC_PREFIX}/web3signer.yaml`;
}

// is the version passed as param the one displayed accoring to the url
function isDisplayedVersion(version,displayedVersion){
  return displayedVersion === version || (displayedVersion === '' && version === defaultVersion);
}

// Update the redoc element and run the redoc display
function updateSpec(version){
  const element = $( `<redoc spec-url="${getSpecPath(version)}"></redoc>` );
  element.appendTo( "main.container" );
  Redoc.init(`${element.attr('spec-url')}?ts=${Date.now()}`, {hideHostname: true});
  updateVersionsDropDown(version);
}

// Update the drop down list and set active version
function updateVersionsDropDown(versionFromUrl){
  $("#versionsList").empty();
  $.each( versions, function( version, infos ) {
    let sourceText = (version === infos.source) ? '' : ` (${infos.source})`;

    let item = $( `<a class="version-number-item dropdown-item" href="?version=${version}">${version}${sourceText}</a>" `);

    if(isDisplayedVersion(version,versionFromUrl)){
      item.addClass("active");
      $('#dropdownMenuButton').text(`${version}${sourceText}`);
      document.title = `Consensys Web3Signer ${SPEC_PREFIX} API Documentation - ${version}`;
    }

    item.appendTo("#versionsList");
  });
}

// get the version in the URL from the search query or hash part (for retro compatibility)
// removes the and # at the beginning of the hash part
// returns empty string if no version present at all
function getVersionFromURL(){
  let params = new URLSearchParams($(location).attr('search').substring(1));
  const version = params.get("version");
  return version != null ? version : $(location).attr('hash').replace(/^[#]/, '');
}

// set the global versions value from the Json file,
// update spec page on completions
function getVersionsFromJsonFile(){
  $.ajaxSetup({ cache: false });
  $.getJSON( 'versions.json', function( data ) {
    if(!jQuery.isEmptyObject(data)){
      versions = data;
    }
  })
  .always(function() { updateSpec(getVersionFromURL()); });
}

// event trigger functions
$(window).on( 'hashchange', function( e ) {
  updateSpec(getVersionFromURL());
});

$(function() {
  getVersionsFromJsonFile();
});

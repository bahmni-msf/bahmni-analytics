#!/bin/bash

read_version(){
  set -e;
  local version=$(awk '/^version/{print $NF}' build.gradle | tr -d \');
  echo $version
}

create_release() {
    local token="$1";
    local tag_name="$2";
    local commit_sha="$3";
    local repo_owner="$4";

    curl -s -S -X POST "https://api.github.com/repos/$repo_owner/bahmni-mart/releases" \
	 -H "Authorization: token $token" \
	 -H "Content-Type: application/json" \
	 -d '{"tag_name": "'"$tag_name"'", "name" : "'"$tag_name"'", "target_commitish":"'"$commit_sha"'" }';
}

upload_asset() {
    set -e;

    local token="$1";
    local name="$2";
    local file="$3";
    local id="$4";
    local repo_owner="$5";

  curl -s -S -X POST "https://uploads.github.com/repos/$repo_owner/bahmni-mart/releases/$id/assets?name=$name" \
	 -H "Accept: application/vnd.github.v3+json" \
	 -H "Authorization: token $token" \
	 -H "Content-Type: application/x-rpm" \
	 --data-binary @"$file";
}

main(){
  # Assign global variables to local variables
    local repo_owner=${args[0]}
    local token=${args[1]};
    local release_name=$(read_version)
    local name="bahmni-mart-${release_name}.noarch.rpm";
    local file="$(pwd)/build/distributions/$name";
    local commit_sha=$(git rev-parse HEAD)

    CREATE_RESPONSE=$(create_release "$token" "$release_name" "$commit_sha" "$repo_owner");
    local release_id=$(echo $CREATE_RESPONSE | python -c 'import json,sys;obj=json.load(sys.stdin);print obj["id"]');
    echo "Created a release with id $release_id"
    echo "Created Response ******************************"
    echo $CREATE_RESPONSE

#    #Upload asset and save the output from curl
    UPLOAD_RESPONSE=$(upload_asset "$token" "$name" "$file" "$release_id" "$repo_owner");
    echo "Upload Response ******************************"
    echo "$UPLOAD_RESPONSE"
}
args=("$@")
main

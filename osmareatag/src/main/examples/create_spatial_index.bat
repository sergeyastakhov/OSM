del national-boundary.idx

call osmosis --read-pbf russia-boundary.osm.pbf --lp --tag-area-content file=tag-index.xml prepareOnly=true --write-null 2>out

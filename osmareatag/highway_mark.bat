call osmosis --read-pbf RU-LEN.osm.pbf --lp --bb clipIncompleteEntities=true --tag-relation-content relationTagName=route relationTagValue=road markEntityTagName=highway insideTag=route:${network}=${name} --tag-area-content --write-xml RU-LEN.hwarea.osm.gz 2>out

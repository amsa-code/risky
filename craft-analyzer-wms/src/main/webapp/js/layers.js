
function addLayers(map) {


    ///////////////////////////////////////////
    // setup the custom wms layer
    ///////////////////////////////////////////

    var wmsUrl = "wms";
    
    var layer1 = new OpenLayers.Layer.WMS( "Drifting",
                wmsUrl, 
                {layers: 'Drifting',transparent: "true", format: "image/png",styles:"Standard"},
                {gutter:15,singleTile:true, visibility:true,opacity: 0.5,animationEnabled: false});
                

//    var marineProfile = new OpenLayers.Layer.WMS("deepshade", 
//                    "http:///osm.franken.de:8080/geoserver/wms",
//                    {layers: "gebco:deepshade", type: 'png', transparent: true},
//                    {isBaseLayer: false, visibility: true, opacity: 0.2, minResolution: 38.22});

    map.addLayer(layer1);
    
    
    ////////////////////////////////////////////////////
    // setup getFeatureInfo on click for all layers
    ////////////////////////////////////////////////////s
    
    var click = new OpenLayers.Control.WMSGetFeatureInfo({
                url: wmsUrl, 
                title: 'Identify features by clicking',
                layers: [layer1],
                queryVisible: true
            })
    click.events.register("getfeatureinfo", this, showInfo);
    map.addControl(click);
    click.activate();
    
}

function showInfo(event) {
    map.addPopup(new OpenLayers.Popup.FramedCloud(
        "chicken", 
        map.getLonLatFromPixel(event.xy),
        null,
        event.text,
        null,
        true
    ));
};

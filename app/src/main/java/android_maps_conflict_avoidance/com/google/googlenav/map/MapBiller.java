package android_maps_conflict_avoidance.com.google.googlenav.map;

import android_maps_conflict_avoidance.com.google.common.geom.Point;
import android_maps_conflict_avoidance.com.google.googlenav.datarequest.DataRequestDispatcher;
import android_maps_conflict_avoidance.com.google.googlenav.map.Map.BillingPointListener;
import android_maps_conflict_avoidance.com.google.map.MapPoint;
import android_maps_conflict_avoidance.com.google.map.MapState;
import android_maps_conflict_avoidance.com.google.map.Zoom;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

class MapBiller {
    private MapPoint lastBilled = null;
    private Zoom lastBilledZoom = null;
    private BillingPointListener listener = null;
    private MapPoint previousBilled = null;
    private Zoom previousBilledZoom = null;

    MapBiller() {
    }

    void doBilling(boolean locationDisplayed, boolean routeDisplayed, Map map) {
        MapState mapState = map.getMapState();
        if (mapState.isSatellite() && !isBillingRequiredForSatellite()) {
            return;
        }
        if (this.lastBilled == null || this.lastBilledZoom != mapState.getZoom()) {
            sendBill(mapState.getCenterPoint(), locationDisplayed, routeDisplayed, map);
            return;
        }
        MapPoint newBillPoint = null;
        Point pixelOffset = map.getPixelOffsetFromCenter(this.lastBilled);
        int pageWidth = Math.min(map.getWidth(), map.getDisplayWidth());
        int pageHeight = Math.min(map.getHeight(), map.getDisplayHeight());
        int halfWidth = pageWidth / 2;
        int halfHeight = pageHeight / 2;
        if (Math.abs(pixelOffset.x) > pageWidth || Math.abs(pixelOffset.y) > pageHeight) {
            newBillPoint = mapState.getCenterPoint();
        } else {
            int dx = 0;
            int dy = 0;
            if (pixelOffset.x < (-halfWidth)) {
                dx = pageWidth;
            } else if (pixelOffset.x > halfWidth) {
                dx = -pageWidth;
            }
            if (pixelOffset.y < (-halfHeight)) {
                dy = pageHeight;
            } else if (pixelOffset.y > halfHeight) {
                dy = -pageHeight;
            }
            if (!(dy == 0 && dx == 0)) {
                newBillPoint = this.lastBilled.pixelOffset(dx, dy, mapState.getZoom());
            }
        }
        if (newBillPoint == null) {
            return;
        }
        if (this.previousBilled == null || this.previousBilledZoom != this.lastBilledZoom || mapState.getCenterPoint().distanceSquared(newBillPoint) < mapState.getCenterPoint().distanceSquared(this.lastBilled)) {
            sendBill(newBillPoint, locationDisplayed, routeDisplayed, map);
        }
    }

    private void sendBill(MapPoint billPoint, boolean locationDisplayed, boolean routeDisplayed, Map map) {
        MapState mapState = map.getMapState();
        this.previousBilled = this.lastBilled;
        this.previousBilledZoom = this.lastBilledZoom;
        this.lastBilled = billPoint;
        this.lastBilledZoom = mapState.getZoom();
        sendBillingPointToServer(billPoint, locationDisplayed, routeDisplayed, map);
        if (this.listener != null) {
            this.listener.billingPointSent(mapState);
        }
    }

    private static boolean isBillingRequiredForSatellite() {
        return Tile.getSatType() == (byte) 6;
    }

    private static void sendBillingPointToServer(MapPoint billPoint, boolean locationDisplayed, boolean routeDisplayed, Map map) {
        MapState mapState = map.getMapState();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        try {
            MapPoint.writePoint(billPoint, dos);
            MapPoint.writePoint(mapState.getCenterPoint(), dos);
            dos.writeShort(mapState.getZoom().getZoomLevel());
            dos.writeInt(map.getLatitudeSpan(mapState));
            dos.writeInt(map.getLongitudeSpan(mapState));
            dos.writeBoolean(locationDisplayed);
            dos.writeBoolean(routeDisplayed);
            DataRequestDispatcher.getInstance().addSimpleRequest(7, baos.toByteArray(), false, false);
        } catch (IOException e) {
            throw new RuntimeException(e.toString());
        }
    }
}

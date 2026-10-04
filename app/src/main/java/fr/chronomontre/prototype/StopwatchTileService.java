package fr.chronomontre.prototype;

import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.wear.protolayout.ActionBuilders;
import androidx.wear.protolayout.ColorBuilders;
import androidx.wear.protolayout.DimensionBuilders;
import androidx.wear.protolayout.LayoutElementBuilders;
import androidx.wear.protolayout.ModifiersBuilders;
import androidx.wear.protolayout.ResourceBuilders;
import androidx.wear.protolayout.TimelineBuilders;
import androidx.wear.tiles.RequestBuilders;
import androidx.wear.tiles.TileBuilders;
import androidx.wear.tiles.TileService;

import com.google.common.util.concurrent.ListenableFuture;

/** A static launcher tile. It never starts, pauses, resets, or reads the stopwatch. */
public final class StopwatchTileService extends TileService {
    private static final String RESOURCE_VERSION = "launcher-1";
    private static final String ICON_ID = "stopwatch";

    @Override protected ListenableFuture<TileBuilders.Tile> onTileRequest(
            RequestBuilders.TileRequest request) {
        return CallbackToFutureAdapter.getFuture(completer -> {
            ActionBuilders.LaunchAction open = new ActionBuilders.LaunchAction.Builder()
                    .setAndroidActivity(new ActionBuilders.AndroidActivity.Builder()
                            .setPackageName(getPackageName())
                            .setClassName(MainActivity.class.getName())
                            .build())
                    .build();

            LayoutElementBuilders.Image icon = new LayoutElementBuilders.Image.Builder()
                    .setResourceId(ICON_ID)
                    .setWidth(DimensionBuilders.dp(56))
                    .setHeight(DimensionBuilders.dp(56))
                    .build();
            LayoutElementBuilders.Text label = new LayoutElementBuilders.Text.Builder()
                    .setText(getString(R.string.tile_open))
                    .setMaxLines(1)
                    .setFontStyle(new LayoutElementBuilders.FontStyle.Builder()
                            .setSize(DimensionBuilders.sp(20))
                            .setColor(ColorBuilders.argb(0xFFFFFFFF))
                            .build())
                    .build();
            LayoutElementBuilders.Column content = new LayoutElementBuilders.Column.Builder()
                    .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                    .addContent(icon)
                    .addContent(new LayoutElementBuilders.Spacer.Builder()
                            .setHeight(DimensionBuilders.dp(14)).build())
                    .addContent(label)
                    .build();
            LayoutElementBuilders.Box root = new LayoutElementBuilders.Box.Builder()
                    .setWidth(DimensionBuilders.expand())
                    .setHeight(DimensionBuilders.expand())
                    .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                    .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                    .setModifiers(new ModifiersBuilders.Modifiers.Builder()
                            .setBackground(new ModifiersBuilders.Background.Builder()
                                    .setColor(ColorBuilders.argb(0xFF000000)).build())
                            .setClickable(new ModifiersBuilders.Clickable.Builder()
                                    .setId("open-stopwatch").setOnClick(open).build())
                            .setSemantics(new ModifiersBuilders.Semantics.Builder()
                                    .setContentDescription(getString(R.string.tile_description)).build())
                            .build())
                    .addContent(content)
                    .build();

            completer.set(new TileBuilders.Tile.Builder()
                    .setResourcesVersion(RESOURCE_VERSION)
                    .setTileTimeline(new TimelineBuilders.Timeline.Builder()
                            .addTimelineEntry(new TimelineBuilders.TimelineEntry.Builder()
                                    .setLayout(new LayoutElementBuilders.Layout.Builder()
                                            .setRoot(root).build())
                                    .build())
                            .build())
                    .build());
            return "ChronoMontre launcher tile";
        });
    }

    @Override protected ListenableFuture<ResourceBuilders.Resources> onTileResourcesRequest(
            RequestBuilders.ResourcesRequest request) {
        return CallbackToFutureAdapter.getFuture(completer -> {
            completer.set(new ResourceBuilders.Resources.Builder()
                    .setVersion(RESOURCE_VERSION)
                    .addIdToImageMapping(ICON_ID, new ResourceBuilders.ImageResource.Builder()
                            .setAndroidResourceByResId(
                                    new ResourceBuilders.AndroidImageResourceByResId.Builder()
                                            .setResourceId(R.drawable.ic_stopwatch).build())
                            .build())
                    .build());
            return "ChronoMontre tile icon";
        });
    }
}

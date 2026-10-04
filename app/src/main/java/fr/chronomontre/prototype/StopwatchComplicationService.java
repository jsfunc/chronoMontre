package fr.chronomontre.prototype;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.RemoteException;
import android.util.Log;

import androidx.wear.watchface.complications.data.ComplicationData;
import androidx.wear.watchface.complications.data.ComplicationType;
import androidx.wear.watchface.complications.data.MonochromaticImage;
import androidx.wear.watchface.complications.data.MonochromaticImageComplicationData;
import androidx.wear.watchface.complications.data.PlainComplicationText;
import androidx.wear.watchface.complications.data.ShortTextComplicationData;
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService;
import androidx.wear.watchface.complications.datasource.ComplicationRequest;

/** Static watch-face shortcut; opening it never changes the stopwatch state. */
public final class StopwatchComplicationService extends ComplicationDataSourceService {
    @Override public void onComplicationRequest(ComplicationRequest request,
            ComplicationRequestListener listener) {
        try {
            listener.onComplicationData(createData(request.getComplicationType(), true));
        } catch (RemoteException error) {
            // The watch face can disappear while its request is being answered.
            Log.w("ChronoMontre", "Watch face disconnected before receiving the shortcut", error);
        }
    }

    @Override public ComplicationData getPreviewData(ComplicationType type) {
        return createData(type, false);
    }

    private ComplicationData createData(ComplicationType type, boolean tappable) {
        MonochromaticImage image = new MonochromaticImage.Builder(
                Icon.createWithResource(this, R.drawable.ic_stopwatch_mono)).build();
        PlainComplicationText description = new PlainComplicationText.Builder(
                getString(R.string.tile_description)).build();

        if (type == ComplicationType.MONOCHROMATIC_IMAGE) {
            MonochromaticImageComplicationData.Builder builder =
                    new MonochromaticImageComplicationData.Builder(image, description);
            if (tappable) builder.setTapAction(openStopwatch());
            return builder.build();
        }
        if (type == ComplicationType.SHORT_TEXT) {
            ShortTextComplicationData.Builder builder = new ShortTextComplicationData.Builder(
                    new PlainComplicationText.Builder(getString(R.string.complication_open)).build(),
                    description).setMonochromaticImage(image);
            if (tappable) builder.setTapAction(openStopwatch());
            return builder.build();
        }
        return null;
    }

    private PendingIntent openStopwatch() {
        Intent intent = new Intent(this, MainActivity.class)
                .setAction(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        return PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}

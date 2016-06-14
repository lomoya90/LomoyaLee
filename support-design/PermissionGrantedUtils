
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

/**
 * Created by liyan36 on 2016-06-14.
 */
public class PermissionGrantedUtils {

    public static void onPermissionGranted(Context context, String permission,
                                           OnPermissionGrantedListener listener) {
        if (ContextCompat.checkSelfPermission(context, permission) !=
                PackageManager.PERMISSION_GRANTED) {
            if (PackageManager.PERMISSION_DENIED ==
                    ActivityCompat.checkSelfPermission(context, permission)) {
                listener.onDenied();
            }

            if (PackageManager.PERMISSION_GRANTED ==
                    ActivityCompat.checkSelfPermission(context, permission)) {
                listener.onGranted();
            }
        } else {
            listener.onGranted();
        }
    }

    public interface OnPermissionGrantedListener {
        void onGranted();

        void onDenied();
    }
}

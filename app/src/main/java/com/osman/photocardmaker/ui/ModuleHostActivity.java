package com.osman.photocardmaker.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.osman.photocardmaker.R;
import com.osman.photocardmaker.core.AppModule;
import com.osman.photocardmaker.core.ModuleRegistry;

/**
 * Generic container that opens any registered module.
 * In later steps real module screens replace the placeholder message.
 */
public class ModuleHostActivity extends Activity {

    public static final String EXTRA_MODULE_ID = "module_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_module_host);

        findViewById(R.id.btnBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        String id = getIntent().getStringExtra(EXTRA_MODULE_ID);
        AppModule module = id == null ? null : ModuleRegistry.findById(id);

        TextView title = (TextView) findViewById(R.id.tvHostTitle);
        TextView message = (TextView) findViewById(R.id.tvHostMessage);

        if (module == null) {
            title.setText(R.string.app_name);
            message.setText(R.string.app_name);
            return;
        }
        title.setText(module.getTitleRes());
        message.setText(getString(R.string.host_registered, module.getStep()));
    }
}

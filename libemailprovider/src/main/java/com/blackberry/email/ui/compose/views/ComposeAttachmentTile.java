package com.blackberry.email.ui.compose.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.blackberry.email.ui.AttachmentTile;
import com.blackberry.lib.emailprovider.R;

public class ComposeAttachmentTile extends AttachmentTile implements AttachmentDeletionInterface {
    private ImageButton mDeleteButton;

    public ComposeAttachmentTile(Context context) {
        this(context, null);
    }

    public ComposeAttachmentTile(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public static ComposeAttachmentTile inflate(LayoutInflater inflater, ViewGroup parent) {
        ComposeAttachmentTile view = (ComposeAttachmentTile) inflater.inflate(
                R.layout.compose_attachment_tile, parent, false);
        return view;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mDeleteButton = (ImageButton) findViewById(R.id.attachment_tile_close_button);
    }

    @Override
    public void addDeleteListener(OnClickListener clickListener) {
        mDeleteButton.setOnClickListener(clickListener);
    }
}

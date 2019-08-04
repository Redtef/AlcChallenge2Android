package com.example.travelmantics;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

public class DealActivity extends AppCompatActivity {
    public static final int PICTURE_RESULT = 42;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;
    EditText text_title;
    EditText text_price;
    EditText text_description;
    TravelDeal travelDeal;
    ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deal);
        //FirebaseUtil.openFirebaseReference("traveldeals",this);
        firebaseDatabase = FirebaseUtil.mFirebaseDatabase;
        databaseReference = FirebaseUtil.mDatabaseReference;
        text_title = (EditText) findViewById(R.id.text_title);
        text_price = (EditText) findViewById(R.id.text_price);
        text_description = (EditText) findViewById(R.id.text_description);
        imageView = findViewById(R.id.image);

        initializeDealFromList();

        Button buttonImage = findViewById(R.id.btnImage);
        buttonImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(intent.createChooser(intent, "insert Picture"), PICTURE_RESULT);
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICTURE_RESULT && resultCode == RESULT_OK) {
            final Uri imageUri = data.getData();
            final StorageReference storageReference = FirebaseUtil.mStorageReference.child(imageUri.getLastPathSegment());
            storageReference.putFile(imageUri).addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Log.d("OnsuccessUpload", "upload successfull");
                    final String imageName = taskSnapshot.getMetadata().getReference().getPath();

                    storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            travelDeal.setImageUrl(uri.toString());
                            travelDeal.setImageName(imageName);
                            showImage(travelDeal.getImageUrl());
                        }
                    });

                }
            });
        }
    }

    private void initializeDealFromList() {
        Intent intent = getIntent();
        TravelDeal travelDeal = (TravelDeal) intent.getSerializableExtra("Deal");
        if (travelDeal == null) {
            travelDeal = new TravelDeal();
        }
        this.travelDeal = travelDeal;

        text_title.setText(travelDeal.getTitle());
        text_description.setText(travelDeal.getDescription());
        text_price.setText(travelDeal.getPrice());
        showImage(travelDeal.getImageUrl());
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.save_menu:
                saveDeal();
                Toast.makeText(this, "Deal saved", Toast.LENGTH_LONG).show();
                clean();
                backToList();
                return true;
            case R.id.delete_menu:
                deleteDeal();
                Toast.makeText(this, "Deal deleted", Toast.LENGTH_LONG).show();
                backToList();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.save_menu, menu);
        if (FirebaseUtil.isAdmin) {
            menu.findItem(R.id.delete_menu).setVisible(true);
            menu.findItem(R.id.save_menu).setVisible(true);
            enableEditText(true);
            findViewById(R.id.btnImage).setEnabled(true);
        } else {
            menu.findItem(R.id.delete_menu).setVisible(false);
            menu.findItem(R.id.save_menu).setVisible(false);
            enableEditText(false);
            findViewById(R.id.btnImage).setEnabled(false);
        }

        return true;
    }


    private void enableEditText(boolean isEnabled) {
        text_price.setEnabled(isEnabled);
        text_description.setEnabled(isEnabled);
        text_title.setEnabled(isEnabled);
    }

    private void saveDeal() {
        travelDeal.setTitle(text_title.getText().toString());
        travelDeal.setDescription(text_description.getText().toString());
        travelDeal.setPrice(text_price.getText().toString());
        if (travelDeal.getId() == null) {
            databaseReference.push().setValue(travelDeal);
        } else {
            databaseReference.child(travelDeal.getId()).setValue(travelDeal);
        }
    }

    private void deleteDeal() {
        if (travelDeal == null) {
            Toast.makeText(this, "Please save the deal before deleting", Toast.LENGTH_LONG).show();
            return;
        }
        databaseReference.child(travelDeal.getId()).removeValue();
        if (travelDeal.getImageName() != null && !travelDeal.getImageName().isEmpty()) {
            StorageReference picStorageReference = FirebaseUtil.mFirebaseStorage.getReference().child(travelDeal.getImageName());
            picStorageReference.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d("delete Image", "sucess");
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d("delete Image", "fail");
                }
            });
        }
    }

    private void backToList() {
        Intent intent = new Intent(this, ListActivity.class);
        startActivity(intent);
    }

    private void clean() {
        text_title.setText("");
        text_description.setText("");
        text_price.setText("");
        text_title.requestFocus();
    }

    //    private void showImage(String url){
//        Log.d("showIMage",url);
//        if(url !=null && !url.isEmpty()  ){
//            Log.d("showIMage","inside");
//            int width = Resources.getSystem().getDisplayMetrics().widthPixels;
//            //Picasso.get().load(url).resize(width,width*2/3).centerCrop().into(imageView);
//            Picasso.get().load(url).resize(100,100).centerCrop().into(imageView);
//        }
//    }
    private void showImage(String url) {
        if (url != null && url.isEmpty() == false) {
            int width = Resources.getSystem().getDisplayMetrics().widthPixels;
            Picasso.get()
                    .load(url)
                    .resize(width, width * 2 / 3)
                    .centerCrop()
                    .into(imageView);
        }
    }
}

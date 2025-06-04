package com.example.camera;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {

    BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);  // layout ini akan berisi BottomNavigationView dan FrameLayout

        bottomNav = findViewById(R.id.bottom_navigation);

        // Set default fragment saat pertama kali dibuka
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }

        // Listener ketika item di navbar diklik
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selected = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                selected = new HomeFragment();
            } else if (itemId == R.id.nav_camera) {
                selected = new CameraFragment();
            } else if (itemId == R.id.nav_article) {
                selected = new ArticleFragment();
            }

            if (selected != null) {
                loadFragment(selected);
                return true;
            }

            return false;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}

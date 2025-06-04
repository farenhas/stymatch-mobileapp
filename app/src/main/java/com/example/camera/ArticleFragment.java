package com.example.camera;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;

public class ArticleFragment extends Fragment {

    public ArticleFragment() {
        // Konstruktor kosong wajib
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate layout fragment_article.xml
        return inflater.inflate(R.layout.fragment_article, container, false);
    }
}

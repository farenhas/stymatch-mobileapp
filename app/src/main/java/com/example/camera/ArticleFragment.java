package com.example.camera;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

public class ArticleFragment extends Fragment {

    public ArticleFragment() {
        // Konstruktor kosong wajib
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate layout fragment_article.xml
        View view = inflater.inflate(R.layout.fragment_article, container, false);

        // Setup click listeners for each card
        setupCardClickListeners(view);

        return view;
    }

    private void setupCardClickListeners(View view) {
        // Inisialisasi CardView berdasarkan ID
        CardView card1 = view.findViewById(R.id.card_1);
        CardView card2 = view.findViewById(R.id.card_2);
        CardView card3 = view.findViewById(R.id.card_3);
        CardView card4 = view.findViewById(R.id.card_4);
        CardView card5 = view.findViewById(R.id.card_5);
        CardView card6 = view.findViewById(R.id.card_6);
        CardView card7 = view.findViewById(R.id.card_7);
        CardView card8 = view.findViewById(R.id.card_8);
        CardView card9 = view.findViewById(R.id.card_9);
        CardView card10 = view.findViewById(R.id.card_10);

        // Set click listeners dengan URL yang telah ditentukan
        card1.setOnClickListener(v -> openUrlInChrome("https://www.warbyparker.com/learn/glasses-for-different-face-shapes"));
        card2.setOnClickListener(v -> openUrlInChrome("https://www.charlottetilbury.com/us/secrets/how-to-apply-blush-to-suit-your-face-shape"));
        card3.setOnClickListener(v -> openUrlInChrome("https://www.thetrendspotter.net/haircuts-for-face-shape/"));
        card4.setOnClickListener(v -> openUrlInChrome("https://scottfsalon.com/best-haircuts-for-different-face-shapes/"));
        card5.setOnClickListener(v -> openUrlInChrome("https://scottfsalon.com/best-haircuts-for-different-face-shapes/"));
        card6.setOnClickListener(v -> openUrlInChrome("https://www.arlowolf.com/face-shapes/"));
        card7.setOnClickListener(v -> openUrlInChrome("https://www.vooglam.com/blogDetail/polygon-glasses-frames-vogue-frames-vooglam?msclkid=9d3b115a07ae13c608857919159e2d89&utm_source=bing&utm_medium=cpc&utm_campaign=Bing-search-DSA-auto&utm_campaignId=530301491&utm_MatchType=b&utm_Network=o&utm_term=vooglam"));
        card8.setOnClickListener(v -> openUrlInChrome("https://www.firstforwomen.com/beauty/makeup/blush-for-face-shape"));
        card9.setOnClickListener(v -> openUrlInChrome("https://www.freshfacebeautyla.com/blog/blush"));
        card10.setOnClickListener(v -> openUrlInChrome("https://laylashine.com/how-to-find-the-perfect-haircut-for-your-face-shape/"));
    }

    private void openUrlInChrome(String url) {
        try {
            // Intent untuk membuka URL di Chrome
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.setPackage("com.android.chrome");

            if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                startActivity(intent);
            } else {
                // Fallback ke browser lain jika Chrome tidak tersedia
                Intent fallbackIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(fallbackIntent);
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback ke browser lain jika terjadi exception
            Intent fallbackIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(fallbackIntent);
        }
    }
}

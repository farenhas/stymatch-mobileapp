package com.example.camera;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;

public class HomeFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_home, container, false);

        View btnSquare = v.findViewById(R.id.btnSquare);
        View btnEgg = v.findViewById(R.id.btnEgg);
        View btnRound = v.findViewById(R.id.btnRound);
        View btnHeart = v.findViewById(R.id.btnHeart);

        btnSquare.setOnClickListener(view -> openFragment(new SquareFragment()));
        btnEgg.setOnClickListener(view -> openFragment(new EggFragment()));
        btnRound.setOnClickListener(view -> openFragment(new RoundFragment()));
        btnHeart.setOnClickListener(view -> openFragment(new HeartFragment()));

        return v;
    }

    private void openFragment(Fragment fragment) {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }
}
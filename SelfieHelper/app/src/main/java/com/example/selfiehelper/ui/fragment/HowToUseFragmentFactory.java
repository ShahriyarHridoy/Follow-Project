package com.example.selfiehelper.ui.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.selfiehelper.R;

public class HowToUseFragmentFactory extends Fragment {

    public static HowToUseFragmentFactory newInstance(int position) {
        Bundle args = new Bundle();
        HowToUseFragmentFactory fragment = new HowToUseFragmentFactory();
        args.putInt("htu_fragment", position);
        fragment.setArguments(args);
        return fragment;
    }



}

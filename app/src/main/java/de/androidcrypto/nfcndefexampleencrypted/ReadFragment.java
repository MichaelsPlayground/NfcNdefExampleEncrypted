package de.androidcrypto.nfcndefexampleencrypted;

import static de.androidcrypto.nfcndefexampleencrypted.Utils.doVibrate;
import static de.androidcrypto.nfcndefexampleencrypted.Utils.playSinglePing;

import android.content.Context;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.Arrays;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ReadFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ReadFragment extends Fragment implements NfcAdapter.ReaderCallback {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public ReadFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ReceiveFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ReadFragment newInstance(String param1, String param2) {
        ReadFragment fragment = new ReadFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    TextView readResult;
    private NfcAdapter mNfcAdapter;
    String dumpExportString = "";
    String tagIdString = "";
    String tagTypeString = "";
    private static final int REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE = 100;
    Context contextSave;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        contextSave = getActivity().getApplicationContext();
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this.getContext());
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        readResult = getView().findViewById(R.id.tvReadResult);
        //doVibrate();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_read, container, false);
    }

    // This method is running in another thread when a card is discovered
    // !!!! This method cannot cannot direct interact with the UI Thread
    // Use `runOnUiThread` method to change the UI from this method
    @Override
    public void onTagDiscovered(Tag tag) {
        // Read and or write to Tag here to the appropriate Tag Technology type class
        // in this example the card should be an Ndef Technology Type

        System.out.println("NFC tag discovered");
        requireActivity().runOnUiThread(() -> {
            readResult.setText("");
        });

        Ndef mNdef = Ndef.get(tag);
        if (mNdef != null) {

            // If we want to read
            // As we did not turn on the NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
            // We can get the cached Ndef message the system read for us.
            NdefMessage mNdefMessage = mNdef.getCachedNdefMessage();
            NdefRecord[] record = mNdefMessage.getRecords();
            int ndefRecordsCount = record.length;
            if (ndefRecordsCount > 0) {
                String ndefText = "";
                for (int i = 0; i < ndefRecordsCount; i++) {
                    short ndefTnf = record[i].getTnf();
                    byte[] ndefType = record[i].getType();
                    byte[] ndefPayload = record[i].getPayload();
                    // here we are trying to parse the content
                    // Well known type - Text
                    if (ndefTnf == NdefRecord.TNF_WELL_KNOWN &&
                            Arrays.equals(ndefType, NdefRecord.RTD_TEXT)) {
                        ndefText = ndefText + "\n" + "rec: " + i +
                                " Well known Text payload\n" + new String(ndefPayload) + " \n";
                        ndefText = ndefText + Utils.parseTextrecordPayload(ndefPayload) + " \n";
                    }
                    // Well known type - Uri
                    if (ndefTnf == NdefRecord.TNF_WELL_KNOWN &&
                            Arrays.equals(ndefType, NdefRecord.RTD_URI)) {
                        ndefText = ndefText + "\n" + "rec: " + i +
                                " Well known Uri payload\n" + new String(ndefPayload) + " \n";
                        ndefText = ndefText + Utils.parseUrirecordPayload(ndefPayload) + " \n";
                    }

                    // TNF 2 Mime Media
                    if (ndefTnf == NdefRecord.TNF_MIME_MEDIA) {
                        ndefText = ndefText + "\n" + "rec: " + i +
                                " TNF Mime Media  payload\n" + new String(ndefPayload) + " \n";
                        ndefText = ndefText + "TNF Mime Media  type\n" + new String(ndefType) + " \n";
                    }
                    // TNF 4 External type
                    if (ndefTnf == NdefRecord.TNF_EXTERNAL_TYPE) {
                        ndefText = ndefText + "\n" + "rec: " + i +
                                " TNF External type payload\n" + new String(ndefPayload) + " \n";
                        ndefText = ndefText + "TNF External type type\n" + new String(ndefType) + " \n";
                    }
                    String finalNdefText = ndefText;
                    getActivity().runOnUiThread(() -> {
                        readResult.setText(finalNdefText);
                    });

                }
            }
        } else {
            getActivity().runOnUiThread(() -> {
                readResult.setText("There was an error in NDEF data");
            });

        }
        doVibrate(getActivity());
        playSinglePing(getContext());
    }

    private void showWirelessSettings() {
        Toast.makeText(this.getContext(), "You need to enable NFC", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mNfcAdapter != null) {

            if (!mNfcAdapter.isEnabled())
                showWirelessSettings();

            Bundle options = new Bundle();
            // Work around for some broken Nfc firmware implementations that poll the card too fast
            options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250);

            // Enable ReaderMode for all types of card and disable platform sounds
            // the option NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK is NOT set
            // to get the data of the tag afer reading
            mNfcAdapter.enableReaderMode(this.getActivity(),
                    this,
                    NfcAdapter.FLAG_READER_NFC_A |
                            NfcAdapter.FLAG_READER_NFC_B |
                            NfcAdapter.FLAG_READER_NFC_F |
                            NfcAdapter.FLAG_READER_NFC_V |
                            NfcAdapter.FLAG_READER_NFC_BARCODE |
                            NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
                    options);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mNfcAdapter != null)
            mNfcAdapter.disableReaderMode(this.getActivity());
    }

}
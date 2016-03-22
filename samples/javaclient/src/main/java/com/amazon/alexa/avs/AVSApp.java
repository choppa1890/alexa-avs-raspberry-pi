/**
 * Copyright 2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * You may not use this file except in compliance with the License. A copy of the License is located the "LICENSE.txt"
 * file accompanying this source. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */
package com.amazon.alexa.avs;

import com.amazon.alexa.avs.auth.AccessTokenListener;
import com.amazon.alexa.avs.auth.AuthSetup;
import com.amazon.alexa.avs.auth.companionservice.RegCodeDisplayHandler;
import com.amazon.alexa.avs.config.DeviceConfig;
import com.amazon.alexa.avs.config.DeviceConfigUtils;
import com.amazon.alexa.avs.http.AVSClientFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

@SuppressWarnings("serial")
public class AVSApp extends JFrame implements ExpectSpeechListener, RecordingRMSListener,
        RegCodeDisplayHandler, AccessTokenListener {

    private static final Logger log = LoggerFactory.getLogger(AVSApp.class);

    private static final String APP_TITLE = "Alexa Voice Service";
    private static final String START_LABEL = "Start Listening";
    private static final String STOP_LABEL = "Stop Listening";
    private static final String PROCESSING_LABEL = "Processing";
    private static final String PREVIOUS_LABEL = "\u21E4";
    private static final String NEXT_LABEL = "\u21E5";
    private static final String PAUSE_LABEL = "\u275A\u275A";
    private static final String PLAY_LABEL = "\u25B6";
    private final AVSController controller;
    private JButton actionButton;
    private JButton playPauseButton;
    private JTextField tokenTextField;
    private JProgressBar visualizer;
    private Thread autoEndpoint = null; // used to auto-endpoint while listening
    private final DeviceConfig deviceConfig;
    // minimum audio level threshold under which is considered silence
    private static final int ENDPOINT_THRESHOLD = 5;
    private static final int ENDPOINT_SECONDS = 2; // amount of silence time before endpointing
    private String accessToken;

    private AuthSetup authSetup;

    public static void main(String[] args) throws Exception {
        if (args.length == 1) {
            new AVSApp(args[0]);
        } else {
            new AVSApp();
        }
    }

    public AVSApp() throws Exception {
        this(DeviceConfigUtils.readConfigFile());
    }

    public AVSApp(String configName) throws Exception {
        this(DeviceConfigUtils.readConfigFile(configName));
    }

    private AVSApp(DeviceConfig config) throws Exception {
        deviceConfig = config;
        controller = new AVSController(this, new AVSAudioPlayerFactory(), new AlertManagerFactory(),
                getAVSClientFactory(deviceConfig), DialogRequestIdAuthority.getInstance());

        authSetup = new AuthSetup(config, this);
        authSetup.addAccessTokenListener(this);
        authSetup.addAccessTokenListener(controller);
        authSetup.startProvisioningThread();

        addDeviceField();
        addTokenField();
        addVisualizerField();
        addActionField();
        addPlaybackButtons();

        getContentPane().setLayout(new GridLayout(0, 1));
        setTitle(getAppTitle());
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(400, 200);
        setVisible(true);
        controller.startHandlingDirectives();
    }

    private String getAppVersion() {
        final Properties properties = new Properties();
        try (final InputStream stream = getClass().getResourceAsStream("/res/version.properties")) {
            properties.load(stream);
            if (properties.containsKey("version")) {
                return properties.getProperty("version");
            }
        } catch (IOException e) {
            log.warn("version.properties file not found on classpath");
        }
        return null;
    }

    private String getAppTitle() {
        String version = getAppVersion();
        String title = APP_TITLE;
        if (version != null) {
            title += " - v" + version;
        }
        return title;
    }

    protected AVSClientFactory getAVSClientFactory(DeviceConfig config) {
        return new AVSClientFactory(config);
    }

    private void addDeviceField() {
        JLabel productIdLabel = new JLabel(deviceConfig.getProductId());
        JLabel dsnLabel = new JLabel(deviceConfig.getDsn());
        productIdLabel.setFont(productIdLabel.getFont().deriveFont(Font.PLAIN));
        dsnLabel.setFont(dsnLabel.getFont().deriveFont(Font.PLAIN));

        FlowLayout flowLayout = new FlowLayout(FlowLayout.LEFT);
        flowLayout.setHgap(0);
        JPanel devicePanel = new JPanel(flowLayout);
        devicePanel.add(new JLabel("Device: "));
        devicePanel.add(productIdLabel);
        devicePanel.add(Box.createRigidArea(new Dimension(5, 0)));
        devicePanel.add(new JLabel("DSN: "));
        devicePanel.add(dsnLabel);
        getContentPane().add(devicePanel);
    }

    private void addTokenField() {
        getContentPane().add(new JLabel("Bearer Token:"));
        tokenTextField = new JTextField(50);
        tokenTextField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                controller.onUserActivity();
                authSetup.onAccessTokenReceived(tokenTextField.getText());
            }
        });
        getContentPane().add(tokenTextField);

        if (accessToken != null) {
            tokenTextField.setText(accessToken);
            accessToken = null;
        }
    }

    private void addVisualizerField() {
        visualizer = new JProgressBar(0, 100);
        getContentPane().add(visualizer);
    }

    private void addActionField() {
        final RecordingRMSListener rmsListener = this;
        actionButton = new JButton(START_LABEL);
        actionButton.setEnabled(true);
        actionButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                controller.onUserActivity();
                if (actionButton.getText().equals(START_LABEL)) { // if in idle mode
                    actionButton.setText(STOP_LABEL);

                    RequestListener requestListener = new RequestListener() {

                        @Override
                        public void onRequestSuccess() {
                            finishProcessing();
                        }

                        @Override
                        public void onRequestError(Throwable e) {
                            log.error("An error occured creating speech request", e);
                            JOptionPane.showMessageDialog(getContentPane(), e.getMessage(), "Error",
                                    JOptionPane.ERROR_MESSAGE);
                            actionButton.doClick();
                            finishProcessing();
                        }
                    };

                    controller.startRecording(rmsListener, requestListener);
                } else { // else we must already be in listening
                    actionButton.setText(PROCESSING_LABEL); // go into processing mode
                    actionButton.setEnabled(false);
                    visualizer.setIndeterminate(true);
                    controller.stopRecording(); // stop the recording so the request can complete
                }
            }
        });

        getContentPane().add(actionButton);
    }

    /**
     * Respond to a music button press event
     *
     * @param action
     *            Playback action to handle
     */
    private void musicButtonPressedEventHandler(final PlaybackAction action) {
        SwingWorker<Void, Void> alexaCall = new SwingWorker<Void, Void>() {
            @Override
            public Void doInBackground() throws Exception {
                visualizer.setIndeterminate(true);
                controller.handlePlaybackAction(action);
                return null;
            }

            @Override
            public void done() {
                visualizer.setIndeterminate(false);
            }
        };
        alexaCall.execute();
    }

    private void createMusicButton(JPanel container, String label, final PlaybackAction action) {
        JButton button = new JButton(label);
        button.setEnabled(true);
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                controller.onUserActivity();
                musicButtonPressedEventHandler(action);
            }
        });
        container.add(button);
    }

    /**
     * Add music control buttons
     */
    private void addPlaybackButtons() {
        JPanel container = new JPanel();
        container.setLayout(new GridLayout(1, 5));

        playPauseButton = new JButton(PLAY_LABEL + "/" + PAUSE_LABEL);
        playPauseButton.setEnabled(true);
        playPauseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                controller.onUserActivity();
                if (controller.isPlaying()) {
                    musicButtonPressedEventHandler(PlaybackAction.PAUSE);
                } else {
                    musicButtonPressedEventHandler(PlaybackAction.PLAY);
                }
            }
        });

        createMusicButton(container, PREVIOUS_LABEL, PlaybackAction.PREVIOUS);
        container.add(playPauseButton);

        createMusicButton(container, NEXT_LABEL, PlaybackAction.NEXT);
        getContentPane().add(container);
    }

    public void finishProcessing() {
        actionButton.setText(START_LABEL);
        actionButton.setEnabled(true);
        visualizer.setIndeterminate(false);
        controller.processingFinished();

    }

    @Override
    public void rmsChanged(int rms) { // AudioRMSListener callback
        // if greater than threshold or not recording, kill the autoendpoint thread
        if ((rms == 0) || (rms > ENDPOINT_THRESHOLD)) {
            if (autoEndpoint != null) {
                autoEndpoint.interrupt();
                autoEndpoint = null;
            }
        } else if (rms < ENDPOINT_THRESHOLD) {
            // start the autoendpoint thread if it isn't already running
            if (autoEndpoint == null) {
                autoEndpoint = new Thread() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(ENDPOINT_SECONDS * 1000);
                            actionButton.doClick(); // hit stop if we get through the autoendpoint
                                                    // time
                        } catch (InterruptedException e) {
                            return;
                        }
                    }
                };
                autoEndpoint.start();
            }
        }

        visualizer.setValue(rms); // update the visualizer
    }

    @Override
    public void onExpectSpeechDirective() {
        Thread thread = new Thread() {
            @Override
            public void run() {
                while (!actionButton.isEnabled() || !actionButton.getText().equals(START_LABEL)
                        || controller.isSpeaking()) {
                    try {
                        Thread.sleep(500);
                    } catch (Exception e) {
                    }
                }
                actionButton.doClick();
            }
        };
        thread.start();

    }

    public void showDialog(String message) {
        JTextArea textMessage = new JTextArea(message);
        textMessage.setEditable(false);
        JOptionPane.showMessageDialog(getContentPane(), textMessage, "Information",
                JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void displayRegCode(String regCode) {
        String regUrl =
                deviceConfig.getCompanionServiceInfo().getServiceUrl() + "/provision/" + regCode;
        showDialog("Please register your device by visiting the following website on "
                + "any system and following the instructions:\n" + regUrl
                + "\n\n Hit OK once completed.");
    }

    @Override
    public synchronized void onAccessTokenReceived(String accessToken) {
        if (tokenTextField == null) {
            this.accessToken = accessToken;
        } else {
            tokenTextField.setText(accessToken);
        }
    }

}

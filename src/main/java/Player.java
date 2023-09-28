import javazoom.jl.decoder.*;
import javazoom.jl.player.AudioDevice;
import javazoom.jl.player.FactoryRegistry;
import support.PlayerWindow;
import support.Song;
import support.Playlist;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.FileNotFoundException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Player {
    //The MPEG audio bitstream.
    private Bitstream bitstream;
    //The MPEG audio decoder.
    private Decoder decoder;
    //The AudioDevice where audio samples are written to.
    private AudioDevice device;

    //Importando outras Classes
    private Playlist playlist;
    private PlayerWindow window;
    private Song music;

    //Variáveis necessárias
    private int currentFrame;
    private int currentTime;
    private boolean pause;
    private boolean stop = true;
    private boolean isPlaying;
    private boolean scrobbles;
    private final Lock lock = new ReentrantLock();

    private final ActionListener buttonListenerPlayNow = e -> {
        stopMusic();
        lock.lock();
        music = playlist.get(window.getSelectedSongIndex());            // pega o index da musica
        playlist.setCurrentIndex(window.getSelectedSongIndex());        // adiciona a musica na playlist
        lock.unlock();
        playNow();
    };

    private final ActionListener buttonListenerRemove = e -> modifyQueue(1);

    private final ActionListener buttonListenerAddSong = e -> modifyQueue(0);

    private final ActionListener buttonListenerPlayPause = e ->{
        lock.lock();
        pause = !pause;
        lock.unlock();
    };

    private final ActionListener buttonListenerStop = e -> stopMusic();

    private final ActionListener buttonListenerNext = e -> {
        closeResources();   //FECHA O DECODER E O BITSTREAM
        modifyQueue(2);
    };

    private final ActionListener buttonListenerPrevious =  e -> {
        closeResources();   //FECHA O DECODER E O BITSTREAM
        modifyQueue(3);
    };

    private final ActionListener buttonListenerShuffle = e -> {
        playlist.toggleShuffle(!stop);               // CHAMA A FUNÇÃO DE SHUFFLE
        window.setQueueList(playlist.getDisplayInfo());         // ATUALIZA A JANELA
    };
    private final ActionListener buttonListenerLoop = e -> playlist.toggleLooping();

    // ALTEREI A ORDEM PRA FICAR MAIS LÓGICO A ORDEM DOS PASSOS
    private final MouseInputAdapter scrubberMouseInputAdapter = new MouseInputAdapter() {
        @Override
        // QUANDO O MOUSE É PRESSIONADO APENAS ATUALIZA O VALOR
        public void mousePressed(MouseEvent e) {
            scrobbles = true;                           // FLAG PARA O TEMPO RECEBER O VALOR DO SCRUBBER
            currentTime = window.getScrubberValue();    // ATUALIZANDO O TEMPO COM O VALOR DO SCRUBBER
            windowActualize();                          // ATUALIZA A JANELA
        }
        @Override
        // QUANDO O MOUSE É ARRASTADO APENAS ATUALIZA O VALOR
        public void mouseDragged(MouseEvent e) {
            currentTime = window.getScrubberValue();    // ATUALIZANDO O TEMPO COM O VALOR DO SCRUBBER
            windowActualize();

        }
        @Override
        // QUANDO SOLTA O MOUSE ATUALIZA O TEMPO DA MUSICA
        public void mouseReleased(MouseEvent e) {
            lock.lock();
            currentTime = window.getScrubberValue();                                        // ATUALIZANDO O TEMPO COM O VALOR DO SCRUBBER
            if (currentTime/ music.getMsPerFrame() < currentFrame) {
                try { // RESETA O CURRENT TIME E CRIA UM NOVO BITSTREAM
                    currentFrame = 0;
                    device = FactoryRegistry.systemRegistry().createAudioDevice();
                    device.open(decoder = new Decoder());
                    bitstream = new Bitstream(music.getBufferedInputStream());
                } catch (FileNotFoundException | JavaLayerException exception) {throw new RuntimeException(exception);}
            }
            try {
                skipToFrame((int) (currentTime/music.getMsPerFrame()));                   // TENTA PULAR PRO FRAME ESPECIFICADO
            } catch (BitstreamException ex) {throw new RuntimeException(ex);}
            scrobbles = false;                                                            // DESATIVA A FLAG PRA ATUALIZAR A MUSICA NORMALMENTE
                lock.unlock();
        }
    };

    public Player() {
        EventQueue.invokeLater(() -> playlist = new Playlist());
        String[][] queue = new String[0][];
        EventQueue.invokeLater(() -> window = new PlayerWindow(
                "Tolafy",
                queue,
                buttonListenerPlayNow,
                buttonListenerRemove,
                buttonListenerAddSong,
                buttonListenerShuffle,
                buttonListenerPrevious,
                buttonListenerPlayPause,
                buttonListenerStop,
                buttonListenerNext,
                buttonListenerLoop,
                scrubberMouseInputAdapter)
        );
    }

    //<editor-fold desc="Essential">
    /**
     * @return False if there are no more frames to play.
     */
    private boolean playNextFrame() throws JavaLayerException {
        if (device != null) {
            Header h = bitstream.readFrame();
            if (h == null) return false;

            SampleBuffer output = (SampleBuffer) decoder.decodeFrame(h, bitstream);
            device.write(output.getBuffer(), 0, output.getBufferLength());
            bitstream.closeFrame();
        }
        return true;
    }

    /**
     * @return False if there are no more frames to skip.
     */
    private boolean skipNextFrame() throws BitstreamException {
        Header h = bitstream.readFrame();
        if (h == null) return false;
        bitstream.closeFrame();
        currentFrame++;
        return true;
    }

    /**
     * Skips bitstream to the target frame if the new frame is higher than the current one.
     *
     * @param newFrame Frame to skip to.
     */
    private void skipToFrame(int newFrame) throws BitstreamException {
        int framesToSkip = newFrame - currentFrame;
        boolean condition = true;
        while (framesToSkip-- > 0 && condition) condition = skipNextFrame();
    }
    //</editor-fold>
    // FUNÇÃO PARA INICIALIZAR A MÚSICA
    private void playNow() {
        closeResources();   // FECHA O DECODER E O BITSTREAM
        pause = false;      // REINICIA FLAG PAUSE
        stop = false;       // REINICIA FLAG STOP
        currentFrame = 0;   // DEFINE O FRAME ATUAL PARA 0
        buttonsON();        // INICIALIZA OS BOTÕES
        try {               //INICIALIZAÇÃO DO DEVICE, DECODER E BITSTREAM
            device = FactoryRegistry.systemRegistry().createAudioDevice();
            device.open(decoder = new Decoder());
            bitstream = new Bitstream(music.getBufferedInputStream());
        } catch (FileNotFoundException | JavaLayerException e) {throw new RuntimeException(e);}
        playing();          // INICIA A THREAD PRINCIPAL (TOCAR MUSICA)
    }

    // FUNÇÃO PARA RODAR A MÚSICA
    private void playing(){
        isPlaying = true;   // FLAG PARA SINALIZAR QUE A MÚSICA ESTÁ TOCANDO
        Thread consumerThread = new Thread(()-> {
            while (isPlaying){
                if(!scrobbles) windowActualize();       // ATUALIZA A JANELA COM NOVAS INFORMAÇÕES DESDE QUE NAO ESTEJA ARRASTANDO
                if (stop) isPlaying = false;            // FLAG PARA ENCERRAR O LOOP
                else if (!pause) {
                    try {
                        isPlaying = playNextFrame();    // TOCA A MÚSICA (isPlaying atualiza com playnextframe)
                        currentFrame++;                 // ATUALIZA O FRAME
                    } catch (JavaLayerException e) {throw new RuntimeException(e);}
                }
            }
            closeResources();           // FECHA O DECODER E O BITSTREAM
            if (!stop) modifyQueue(2); // SE NÃO PAROU POR STOP, TOCA A PRÓXIMA MUSICA (testar pra ver se nao tem bug)
        });
        consumerThread.start();
    }

    // FUNÇÃO PARA A THREAD DO PRODUTOR (GERADOR DAS LISTAS)
    private void modifyQueue(int x){
        Thread producerThread = new Thread(()->{
            // ADD MUSIC
            if (x == 0){
                Song file = window.openFileChooser();               // ENCONTRA O LOCAL DO ARQUIVO
                lock.lock();
                playlist.add(file);                                 // ADD A MUSICA NA LISTA DE MUSICAS
                window.setQueueList(playlist.getDisplayInfo());     // ATUALIZA O DISPLAY
                lock.unlock();
            }
            // REMOVE MUSIC
            else if (x == 1){ // RESOLVER BUG
                lock.lock();
                int removed = playlist.remove(window.getSelectedSongIndex());   // CAPTURA A FLAG DE REMOÇÃO DE MÚSICA
                window.setQueueList(playlist.getDisplayInfo());                 // ATUALIZA A JANELA
                if (removed == 2 && playlist.hasNext()) {                   // SE A MÚSICA QUE FOI REMOVIDA ESTAVA TOCANDO
                    stopMusic();                                            // PARA A REPRODUÇÃO
                    if (playlist.hasNext()){                                // SE TIVER MAIS MUSICA PRA TOCAR VAI TOCAR
                        music = playlist.get(playlist.getCurrentIndex());   // COMO A MUSICA FOI REMOVIDA DA LISTA, ENTAO O SEU INDEX JA EH O DA PROXIMA MUSICA
                        playNow();
                    }
                }
                lock.unlock();
            }
            // NEXT MUSIC
            else if (x == 2){
                if (playlist.hasNext()) {
                    lock.lock();
                    int nextIdx = playlist.getNextIndex();   // CAPTURA O INDEX DA PRÓXIMA MÚSICA
                    music = playlist.get(nextIdx);           // ATUALIZA A MÚSICA
                    playlist.setCurrentIndex(nextIdx);       // ATUALIZA O INDEX ATUAL
                    lock.unlock();
                    playNow();
                }
                else
                    stop = true;
            }
            // PREV MUSIC
            else if (x == 3){
                if (playlist.hasPrevious()) {
                    lock.lock();
                    int prevIdx = playlist.getPreviousIndex();  // CAPTURA O INDEX DA MÚSICA ANTERIOR
                    music = playlist.get(prevIdx);              // ATUALIZA A MÚSICA
                    playlist.setCurrentIndex(prevIdx);          // ATUALIZA O INDEX ATUAL
                    lock.unlock();
                    playNow();
                }
            }
            // SEMPRE QUE UMA MUSICA É ADICIONADA OU REMOVIDA VERIFICA OS BOTÕES DE LOOP E SHUFFLE
            window.setEnabledLoopButton(!playlist.isEmpty());
            window.setEnabledShuffleButton(!playlist.isEmpty());
        });
        producerThread.start();
    }
    private void stopMusic(){
        lock.lock();
        currentFrame = 0;           // REINICIA O FRAME
        stop = true;                // FLAG PARA PARAR O LOOP DO PLAYING
        closeResources();           // FECHA O DECODER E BITSTREAM
        window.resetMiniPlayer();   // DESLIGA TODOS OS BOTÕES
        lock.unlock();
    }

    // HABILITA BOTÕES E NOME DA MÚSICA
    private void buttonsON(){
        window.setEnabledPlayPauseButton(true);
        window.setEnabledStopButton(true);
        window.setEnabledScrubber(true);
        window.setPlayingSongInfo(music.getTitle(), music.getAlbum(), music.getArtist());
    }

    // ATUALIZA A JANELA
    private void windowActualize(){
        // ALGORITMO PARA O TEMPO DA MÚSICA
        if (!scrobbles) currentTime = (int) (currentFrame * music.getMsPerFrame()); // ATUALIZA O TEMPO DESDE QUE NAO ESTEJA ARRASTANDO
        int totalTime = (int) (music.getNumFrames() * music.getMsPerFrame());
        window.setTime(currentTime,totalTime);
        // BOTÕES SITUACIONAIS (DEPENDEM DE ALGO QUE ESTARÁ RODANDO)
        window.setPlayPauseButtonIcon(pause ? 0 : 1);
        window.setEnabledNextButton(playlist.hasNext());
        window.setEnabledPreviousButton(playlist.hasPrevious());
        window.setEnabledLoopButton(playlist.size() > 1);
        window.setEnabledShuffleButton(playlist.size() > 1);
    }
    // CORREÇÃO DE BUGS, FECHA O BISTREAM E O DEVICE
    private void closeResources() {
        if (bitstream != null) {
            try {bitstream.close();
            } catch (BitstreamException ignored) {}
        }
        if (device != null) device.close();
    }
}

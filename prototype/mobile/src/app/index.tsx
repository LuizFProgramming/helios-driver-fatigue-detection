import { useEffect, useState } from 'react';
import {
  ActivityIndicator,
  Alert,
  Image,
  SafeAreaView,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  Vibration,
  View,
} from 'react-native';

export default function App() {
  // Estado para navegação entre telas: 'home', 'config', 'monitoring'
  const [telaAtual, setTelaAtual] = useState('home');

  // Configurações da API
  const [apiUrl, setApiUrl] = useState('http://192.168.1.15:8000');
  const [deviceId, setDeviceId] = useState('ESP32CAM_01');
  const [apiOnline, setApiOnline] = useState(false);
  const [carregandoStatus, setCarregandoStatus] = useState(false);

  // Dados do Monitoramento
  const [ultimoFrame, setUltimoFrame] = useState(null);
  const [carregandoFrame, setCarregandoFrame] = useState(false);
  const [autoAtualizar, setAutoAtualizar] = useState(false);

  // Checa se a API está online ao alterar a URL ou abrir o app
  useEffect(() => {
    verificarStatusApi();
  }, [apiUrl]);

  // Polling automático no monitoramento (a cada 2 segundos)
  useEffect(() => {
    let interval = null;
    if (telaAtual === 'monitoring' && autoAtualizar) {
      interval = setInterval(() => {
        buscarUltimoFrame();
      }, 2000);
    } else {
      clearInterval(interval);
    }
    return () => clearInterval(interval);
  }, [telaAtual, autoAtualizar, apiUrl, deviceId]);

  // Testa conexão com a API
  const verificarStatusApi = async () => {
    setCarregandoStatus(true);
    try {
      const urlLimpa = apiUrl.replace(/\/$/, '');
      const response = await fetch(`${urlLimpa}/`, { method: 'GET' });
      if (response.ok) {
        setApiOnline(true);
      } else {
        setApiOnline(false);
      }
    } catch (error) {
      setApiOnline(false);
    } finally {
      setCarregandoStatus(false);
    }
  };

  // Buscar o último frame recebido pela API
  const buscarUltimoFrame = async () => {
    setCarregandoFrame(true);
    try {
      const urlLimpa = apiUrl.replace(/\/$/, '');
      const response = await fetch(
        `${urlLimpa}/frames/latest?device_id=${deviceId}`
      );

      if (response.ok) {
        const data = await response.json();
        setUltimoFrame(data.frame);

        // 🔔 Se detectar sonolência, o celular vibra!
        if (data.frame && data.frame.status_motorista === 'Sonolência Detectada') {
          // Padrão de vibração de emergência: [espera, vibra, espera, vibra]
          Vibration.vibrate([0, 500, 200, 500]);
        }
      } else {
        setUltimoFrame(null);
      }
    } catch (error) {
      console.log('Erro ao buscar frame:', error);
    } finally {
      setCarregandoFrame(false);
    }
  };

  // ==========================================
  // TELA 1: INICIAL / HOME
  // ==========================================
  if (telaAtual === 'home') {
    return (
      <SafeAreaView style={styles.container}>
        <StatusBar barStyle="light-content" backgroundColor="#0F172A" />
        <ScrollView contentContainerStyle={styles.scrollContainer}>
          <View style={styles.headerBox}>
            <Text style={styles.tituloHeader}>HELIOS</Text>
            <Text style={styles.subtituloHeader}>Sistema de Monitoramento</Text>
          </View>

          <View style={styles.cardStatus}>
            <Text style={styles.cardLabel}>Status do Servidor API</Text>
            <View style={styles.statusRow}>
              {carregandoStatus ? (
                <ActivityIndicator color="#FF9F1C" size="small" />
              ) : (
                <>
                  <View
                    style={[
                      styles.bolinhaStatus,
                      { backgroundColor: apiOnline ? '#2EC4B6' : '#E71D36' },
                    ]}
                  />
                  <Text style={styles.statusTexto}>
                    {apiOnline ? 'Conectado' : 'Desconectado'}
                  </Text>
                </>
              )}
            </View>
          </View>

          <View style={styles.menuBotoes}>
            <TouchableOpacity
              style={[
                styles.botaoPrincipal,
                !apiOnline && styles.botaoDesabilitado,
              ]}
              disabled={!apiOnline}
              onPress={() => {
                buscarUltimoFrame();
                setTelaAtual('monitoring');
              }}
            >
              <Text style={styles.textoBotaoPrincipal}>Abrir Monitoramento</Text>
            </TouchableOpacity>

            <TouchableOpacity
              style={styles.botaoSecundario}
              onPress={() => setTelaAtual('config')}
            >
              <Text style={styles.textoBotaoSecundario}>Configurações</Text>
            </TouchableOpacity>
          </View>
        </ScrollView>
      </SafeAreaView>
    );
  }

  // ==========================================
  // TELA 2: CONFIGURAÇÕES
  // ==========================================
  if (telaAtual === 'config') {
    return (
      <SafeAreaView style={styles.container}>
        <StatusBar barStyle="light-content" backgroundColor="#0F172A" />
        <ScrollView contentContainerStyle={styles.scrollContainer}>
          <Text style={styles.tituloTela}>Configurações</Text>

          <View style={styles.inputGroup}>
            <Text style={styles.inputLabel}>URL da API Backend</Text>
            <TextInput
              style={styles.input}
              value={apiUrl}
              onChangeText={setApiUrl}
              placeholder="http://192.168.x.x:8000"
              placeholderTextColor="#64748B"
              autoCapitalize="none"
              autoCorrect={false}
            />
          </View>

          <View style={styles.inputGroup}>
            <Text style={styles.inputLabel}>ID do Dispositivo</Text>
            <TextInput
              style={styles.input}
              value={deviceId}
              onChangeText={setDeviceId}
              placeholder="Ex: ESP32CAM_01"
              placeholderTextColor="#64748B"
              autoCapitalize="characters"
              autoCorrect={false}
            />
          </View>

          <TouchableOpacity
            style={styles.botaoTeste}
            onPress={async () => {
              await verificarStatusApi();
              Alert.alert(
                'Status da Conexão',
                apiOnline ? 'Conectado com sucesso!' : 'Não foi possível conectar à API.'
              );
            }}
          >
            {carregandoStatus ? (
              <ActivityIndicator color="#0F172A" />
            ) : (
              <Text style={styles.textoBotaoTeste}>Testar Conexão</Text>
            )}
          </TouchableOpacity>

          <TouchableOpacity
            style={styles.botaoVoltar}
            onPress={() => setTelaAtual('home')}
          >
            <Text style={styles.textoBotaoVoltar}>Voltar para o Início</Text>
          </TouchableOpacity>
        </ScrollView>
      </SafeAreaView>
    );
  }

  // ==========================================
  // TELA 3: MONITORAMENTO
  // ==========================================
  if (telaAtual === 'monitoring') {
    const urlImagem = ultimoFrame
      ? `${apiUrl.replace(/\/$/, '')}/frames/${ultimoFrame.frame_id}?device_id=${deviceId}&t=${Date.now()}`
      : null;

    const temSonolencia =
      ultimoFrame && ultimoFrame.status_motorista === 'Sonolência Detectada';

    return (
      <SafeAreaView style={styles.container}>
        <StatusBar barStyle="light-content" backgroundColor="#0F172A" />
        <ScrollView contentContainerStyle={styles.scrollContainer}>
          <View style={styles.topoMonitoramento}>
            <TouchableOpacity onPress={() => setTelaAtual('home')}>
              <Text style={styles.linkVoltar}>← Voltar</Text>
            </TouchableOpacity>
            <Text style={styles.tituloTela}>Painel ao Vivo</Text>
            <View style={{ width: 40 }} />
          </View>

          {/* Banner de alerta visual em caso de sonolência */}
          {temSonolencia && (
            <View style={styles.bannerAlerta}>
              <Text style={styles.textoBannerAlerta}>⚠️ SONOLÊNCIA DETECTADA!</Text>
            </View>
          )}

          <View style={styles.areaImagem}>
            {carregandoFrame ? (
              <ActivityIndicator color="#FF9F1C" size="large" />
            ) : urlImagem ? (
              <Image
                source={{ uri: urlImagem }}
                style={styles.imagemFrame}
                resizeMode="contain"
              />
            ) : (
              <View style={styles.placeholderImagem}>
                <Text style={styles.textoPlaceholder}>
                  Nenhum frame recebido até o momento.
                </Text>
              </View>
            )}
          </View>

          {ultimoFrame && (
            <View style={[styles.cardInfo, temSonolencia && styles.cardInfoAlerta]}>
              {ultimoFrame.status_motorista && (
                <Text style={styles.infoText}>
                  Status:{' '}
                  <Text
                    style={[
                      styles.infoBold,
                      temSonolencia ? styles.textoAlerta : styles.textoNormal,
                    ]}
                  >
                    {ultimoFrame.status_motorista}
                  </Text>
                </Text>
              )}

              <Text style={styles.infoText}>
                ID do Frame: <Text style={styles.infoBold}>{ultimoFrame.frame_id}</Text>
              </Text>

              {ultimoFrame.ear !== undefined && (
                <Text style={styles.infoText}>
                  EAR: <Text style={styles.infoBold}>{ultimoFrame.ear.toFixed(2)}</Text>
                </Text>
              )}
              {ultimoFrame.mar !== undefined && (
                <Text style={styles.infoText}>
                  MAR: <Text style={styles.infoBold}>{ultimoFrame.mar.toFixed(2)}</Text>
                </Text>
              )}
              {ultimoFrame.perclos !== undefined && (
                <Text style={styles.infoText}>
                  PERCLOS: <Text style={styles.infoBold}>{ultimoFrame.perclos}%</Text>
                </Text>
              )}
              {ultimoFrame.timestamp && (
                <Text style={styles.infoText}>
                  Horário: <Text style={styles.infoBold}>{ultimoFrame.timestamp}</Text>
                </Text>
              )}
            </View>
          )}

          <TouchableOpacity
            style={styles.botaoAtualizar}
            onPress={buscarUltimoFrame}
          >
            <Text style={styles.textoBotaoAtualizar}>Atualizar Frame Agora</Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={[styles.botaoAuto, autoAtualizar && styles.botaoAutoAtivo]}
            onPress={() => setAutoAtualizar(!autoAtualizar)}
          >
            <Text style={styles.textoBotaoAuto}>
              {autoAtualizar ? '■ Parar Auto-Atualização' : '▶ Iniciar Auto-Atualização'}
            </Text>
          </TouchableOpacity>
        </ScrollView>
      </SafeAreaView>
    );
  }
}

// ==========================================
// ESTILOS VISUAIS
// ==========================================
const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#0F172A',
    paddingHorizontal: 20,
    paddingTop: 40,
  },
  scrollContainer: {
    paddingBottom: 30,
  },
  headerBox: {
    alignItems: 'center',
    marginVertical: 30,
  },
  tituloHeader: {
    fontSize: 36,
    fontWeight: 'bold',
    color: '#FF9F1C',
    letterSpacing: 2,
  },
  subtituloHeader: {
    fontSize: 16,
    color: '#94A3B8',
    marginTop: 4,
  },
  cardStatus: {
    backgroundColor: '#1E293B',
    padding: 20,
    borderRadius: 12,
    marginVertical: 20,
    alignItems: 'center',
  },
  cardLabel: {
    color: '#94A3B8',
    fontSize: 14,
    marginBottom: 8,
  },
  statusRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  bolinhaStatus: {
    width: 14,
    height: 14,
    borderRadius: 7,
    marginRight: 8,
  },
  statusTexto: {
    color: '#FFF',
    fontSize: 18,
    fontWeight: 'bold',
  },
  menuBotoes: {
    marginTop: 20,
    gap: 15,
  },
  botaoPrincipal: {
    backgroundColor: '#FF9F1C',
    padding: 18,
    borderRadius: 12,
    alignItems: 'center',
  },
  botaoDesabilitado: {
    opacity: 0.5,
  },
  textoBotaoPrincipal: {
    color: '#0F172A',
    fontSize: 18,
    fontWeight: 'bold',
  },
  botaoSecundario: {
    backgroundColor: '#334155',
    padding: 18,
    borderRadius: 12,
    alignItems: 'center',
  },
  textoBotaoSecundario: {
    color: '#FFF',
    fontSize: 16,
    fontWeight: '600',
  },
  tituloTela: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#FFF',
    marginBottom: 20,
  },
  inputGroup: {
    marginBottom: 20,
  },
  inputLabel: {
    color: '#94A3B8',
    marginBottom: 8,
    fontSize: 14,
  },
  input: {
    backgroundColor: '#1E293B',
    color: '#FFF',
    padding: 15,
    borderRadius: 8,
    fontSize: 16,
    borderWidth: 1,
    borderColor: '#334155',
  },
  botaoTeste: {
    backgroundColor: '#2EC4B6',
    padding: 16,
    borderRadius: 8,
    alignItems: 'center',
    marginTop: 10,
  },
  textoBotaoTeste: {
    color: '#0F172A',
    fontWeight: 'bold',
    fontSize: 16,
  },
  botaoVoltar: {
    marginTop: 20,
    padding: 15,
    alignItems: 'center',
  },
  textoBotaoVoltar: {
    color: '#94A3B8',
    fontSize: 16,
  },
  topoMonitoramento: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  linkVoltar: {
    color: '#FF9F1C',
    fontSize: 16,
  },
  bannerAlerta: {
    backgroundColor: '#E71D36',
    padding: 12,
    borderRadius: 8,
    marginVertical: 10,
    alignItems: 'center',
  },
  textoBannerAlerta: {
    color: '#FFF',
    fontWeight: 'bold',
    fontSize: 16,
  },
  areaImagem: {
    width: '100%',
    height: 250,
    backgroundColor: '#1E293B',
    borderRadius: 12,
    overflow: 'hidden',
    justify: 'center',
    alignItems: 'center',
    marginVertical: 15,
  },
  imagemFrame: {
    width: '100%',
    height: '100%',
  },
  placeholderImagem: {
    padding: 20,
  },
  textoPlaceholder: {
    color: '#64748B',
    fontSize: 14,
    textAlign: 'center',
  },
  cardInfo: {
    backgroundColor: '#1E293B',
    padding: 15,
    borderRadius: 10,
    marginBottom: 15,
  },
  cardInfoAlerta: {
    borderWidth: 2,
    borderColor: '#E71D36',
  },
  infoText: {
    color: '#CBD5E1',
    fontSize: 15,
    marginBottom: 5,
  },
  infoBold: {
    fontWeight: 'bold',
    color: '#FFF',
  },
  textoAlerta: {
    color: '#E71D36',
  },
  textoNormal: {
    color: '#2EC4B6',
  },
  botaoAtualizar: {
    backgroundColor: '#334155',
    padding: 15,
    borderRadius: 8,
    alignItems: 'center',
    marginBottom: 10,
  },
  textoBotaoAtualizar: {
    color: '#FFF',
    fontWeight: '600',
  },
  botaoAuto: {
    backgroundColor: '#1E293B',
    padding: 15,
    borderRadius: 8,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: '#334155',
  },
  botaoAutoAtivo: {
    borderColor: '#2EC4B6',
    backgroundColor: '#0F2A2A',
  },
  textoBotaoAuto: {
    color: '#2EC4B6',
    fontWeight: '600',
  },
});
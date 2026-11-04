package com.fehunter.captureflow.features.settings.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InformationPage(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    BackHandler(onBack = onBack)
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            content()
        }
    }
}

@Composable
private fun InformationSection(title: String, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    InformationPage(title = "Privacidade", onBack = onBack) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "Seu caderno é particular. O CaptureFlow foi desenvolvido para organizar referências, tarefas e anotações no próprio dispositivo.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            item {
                InformationSection(
                    title = "Armazenamento local",
                    body = "Suas referências, títulos, categorias, notas, subtarefas, preferências e imagens salvas pelo aplicativo ficam armazenados localmente no dispositivo. O CaptureFlow não exige uma conta e não oferece sincronização própria com servidores ou nuvem.",
                )
            }
            item {
                InformationSection(
                    title = "Quando a internet é utilizada",
                    body = "Alguns recursos acessam serviços externos quando você escolhe utilizá-los: importar um link pode consultar a página para obter título e imagem; Newsletters carrega feeds; e o leitor integrado ou navegador externo abre sites. Nesses acessos, o site ou serviço consultado pode receber dados técnicos da conexão, como endereço IP, e aplicar sua própria política de privacidade. O conteúdo dessas páginas não é sincronizado pelo CaptureFlow.",
                )
            }
            item {
                InformationSection(
                    title = "Capturas de tela e permissões",
                    body = "Se você ativar o monitoramento de screenshots, o Android poderá conceder ao aplicativo o acesso necessário para detectar novas capturas e oferecer a importação. O processamento e o salvamento da referência acontecem no dispositivo. Você pode desativar esse recurso nas opções do próprio aplicativo ou nas configurações do Android.",
                )
            }
            item {
                InformationSection(
                    title = "Backups: funcionalidade futura",
                    body = "O CaptureFlow ainda não possui uma função integrada de backup. Está planejada uma exportação composta por um arquivo JSON com os dados e arquivos separados para as imagens. Quando estiver disponível, você escolherá onde salvar e será responsável por guardar essa cópia em um local seguro. Até lá, recomendamos não desinstalar o aplicativo nem limpar seus dados sem antes considerar que as informações locais podem ser perdidas.",
                )
            }
            item {
                InformationSection(
                    title = "Seu controle",
                    body = "Você decide o que adicionar ao caderno, quais links abrir e quais permissões conceder. Ao excluir uma referência, o aplicativo também executa a limpeza dos arquivos privados associados quando aplicável.",
                )
            }
            item {
                Text(
                    "Esta descrição apresenta o funcionamento atual do aplicativo e será revisada sempre que novos recursos ou integrações forem adicionados.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private data class TutorialSection(val title: String, val body: String)

@Composable
fun TutorialsScreen(onBack: () -> Unit) {
    val sections = listOf(
        TutorialSection("Adicionar uma referência", "Na Biblioteca, escolha Fotografar para usar a câmera, Galeria para selecionar uma imagem ou Nova tarefa para criar um registro apenas com texto. Revise as informações e escolha a categoria antes de salvar."),
        TutorialSection("Salvar um link", "Compartilhe uma página com o CaptureFlow pelo menu Compartilhar do Android. Quando houver metadados disponíveis, o aplicativo sugere um título e uma imagem; confira os dados antes de concluir."),
        TutorialSection("Organizar por categorias", "Use os filtros de categoria para encontrar referências. No gerenciamento de categorias, crie, renomeie ou organize suas categorias. A busca fica logo abaixo dos filtros."),
        TutorialSection("Pesquisar e ordenar", "Digite parte do título na busca. Dentro de uma categoria, use as opções de ordenação para alternar entre data e título. Os filtros ajudam a localizar o que ainda está pendente ou já foi concluído."),
        TutorialSection("Concluir uma tarefa", "Abra uma referência e marque a caixa de seleção para concluí-la. Use a sub-checklist para dividir uma tarefa em etapas menores; você pode marcar ou remover cada item."),
        TutorialSection("Adicionar anotações", "No visualizador, toque no ícone de balão para criar anotações vinculadas à referência. É possível editar ou remover um bloco existente mantendo-o pressionado."),
        TutorialSection("Visualizar uma referência", "Toque em um card para abrir o visualizador. Deslize horizontalmente para navegar entre referências e, quando houver várias imagens, use os controles ou o gesto vertical para alternar entre elas."),
        TutorialSection("Abrir conteúdo da web", "Quando uma referência possui um link compatível, toque no ícone de globo na barra vertical para tentar visualizar a página dentro do CaptureFlow. No leitor, use a opção de abrir externamente se o site exigir login, JavaScript ou recursos não suportados."),
        TutorialSection("Newsletters", "Use o ícone de RSS no cabeçalho para acessar Newsletters e consultar os feeds adicionados. O carregamento dos feeds depende de conexão com a internet."),
        TutorialSection("Monitoramento de screenshots", "O ícone de notificações no cabeçalho permite configurar ou ativar o aviso de novas capturas de tela. O Android solicitará a permissão necessária; você pode desativar o monitoramento quando quiser."),
        TutorialSection("Tema do aplicativo", "No menu de opções, escolha Alterar tema para seguir o dispositivo ou usar o tema claro ou escuro."),
    )
    InformationPage(title = "Tutoriais", onBack = onBack) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    "Um guia rápido para aproveitar os recursos do seu caderno.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(sections) { section ->
                InformationSection(title = section.title, body = section.body)
            }
        }
    }
}

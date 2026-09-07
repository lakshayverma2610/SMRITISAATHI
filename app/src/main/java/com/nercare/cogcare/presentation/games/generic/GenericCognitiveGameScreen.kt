package com.nercare.cogcare.presentation.games.generic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nercare.cogcare.presentation.theme.*
import androidx.hilt.navigation.compose.hiltViewModel

data class GameQuestion(
    val prompt: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

@Composable
fun GenericCognitiveGameScreen(
    patientId: String,
    gameId: String,
    gameTitle: String,
    domain: String,
    onBack: () -> Unit,
    viewModel: PersonalizedGameViewModel = hiltViewModel()
) {
    val questions by viewModel.questions.collectAsState()
    LaunchedEffect(patientId, gameId) { viewModel.load(patientId, gameId) }
    var currentStep by remember { mutableStateOf(0) }
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    var isSubmitted by remember { mutableStateOf(false) }
    var score by remember { mutableStateOf(0) }
    var isGameFinished by remember { mutableStateOf(false) }

    val currentQ = questions.getOrNull(currentStep)

    fun onOptionSelected(index: Int) {
        if (!isSubmitted) {
            selectedOption = index
            isSubmitted = true
            if (index == currentQ?.correctIndex) {
                score += 1
            }
        }
    }

    fun onNext() {
        if (currentStep + 1 < questions.size) {
            currentStep += 1
            selectedOption = null
            isSubmitted = false
        } else {
            isGameFinished = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundCream)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = PrimaryGreen)
                }

                Surface(
                    color = SecondaryGreen,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = domain,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimaryGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = gameTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = PrimaryGreen,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (!isGameFinished && currentQ != null) {
                // Progress indicator
                LinearProgressIndicator(
                    progress = (currentStep + 1).toFloat() / questions.size,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = PrimaryGreen,
                    trackColor = SecondaryGreen
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Question ${currentStep + 1} of ${questions.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondaryMuted
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Question Prompt Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SecondaryGreen)
                ) {
                    Text(
                        text = currentQ.prompt,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryGreen,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Options List
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    currentQ.options.forEachIndexed { index, option ->
                        val isSelected = selectedOption == index
                        val isCorrect = isSubmitted && index == currentQ.correctIndex
                        val isWrong = isSubmitted && isSelected && index != currentQ.correctIndex

                        val containerColor = when {
                            isCorrect -> AccentMint.copy(alpha = 0.3f)
                            isWrong -> ErrorRed.copy(alpha = 0.15f)
                            isSelected -> SecondaryGreen
                            else -> SurfaceWhite
                        }

                        val borderColor = when {
                            isCorrect -> PrimaryGreen
                            isWrong -> ErrorRed
                            else -> SecondaryGreen
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable(enabled = !isSubmitted) { onOptionSelected(index) },
                            shape = RoundedCornerShape(16.dp),
                            color = containerColor,
                            border = androidx.compose.foundation.BorderStroke(2.dp, borderColor)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = option,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isWrong) ErrorRed else PrimaryGreen,
                                    modifier = Modifier.weight(1f)
                                )

                                if (isCorrect) {
                                    Icon(Icons.Default.CheckCircle, null, tint = PrimaryGreen)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Next Button
                AnimatedVisibility(visible = isSubmitted) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = currentQ.explanation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondaryMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Button(
                            onClick = { onNext() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                        ) {
                            Text(
                                text = if (currentStep + 1 < questions.size) "Next Question →" else "Complete Exercise 🎉",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            } else {
                // Game Finished Celebration
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        modifier = Modifier.size(88.dp),
                        shape = CircleShape,
                        color = SecondaryGreen
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = WarningYellow,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Wonderful Work! 🌟",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "You scored $score out of ${questions.size} in $gameTitle!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondaryMuted,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(36.dp))

                    Button(
                        onClick = onBack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                    ) {
                        Text("Back to Game Hub 🌿", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

internal fun getQuestionsForGame(gameId: String): List<GameQuestion> {
    return when (gameId) {
        "spot_change" -> listOf(
            GameQuestion("Look closely: Which item was in the tea cup this morning?", listOf("Tea Leaves & Cardamom", "Orange Juice", "Coffee Beans"), 0, "Cardamom tea is a warm morning favorite! ☕"),
            GameQuestion("In the village picture, what color was the roof?", listOf("Bright Red", "Sky Blue", "Silver Straw"), 0, "Red terracotta roof tile was noted! 🏡"),
            GameQuestion("Which tree grew beside the river bank?", listOf("Peepal Tree", "Coconut Tree", "Banyan Tree"), 2, "The great Banyan tree by the water! 🌳")
        )
        "life_story_recall" -> listOf(
            GameQuestion("Which university did you attend during your youth?", listOf("Gauhati University", "Delhi University", "Calcutta University"), 0, "Preserved in your Life Story milestones! 🎓"),
            GameQuestion("What was your favorite celebration feast?", listOf("Bihu Pitha & Laroo", "Chocolate Cake", "Pizza"), 0, "Traditional celebrations bring warmth! 🌿"),
            GameQuestion("Who is your primary family doctor?", listOf("Dr. Sharma", "Dr. Gupta", "Dr. Paul"), 0, "Dr. Sharma manages your health checkups.")
        )
        "familiar_music" -> listOf(
            GameQuestion("Complete the famous song melody: 'Manuhe Manuhor Babe...'", listOf("Jodihe Akonou Nabhabe", "Aamar Gaon", "Ei Desh Aamar"), 0, "Bhupen Hazarika's iconic melody! 🎵"),
            GameQuestion("Which musical instrument uses twin drums with leather tops?", listOf("Tabla", "Flute", "Harmonium"), 0, "Tabla rhythm accompanies classical tunes! 🥁"),
            GameQuestion("When is the festive Bihu dhol played most joyfully?", listOf("Spring / Rongali", "Winter Midnight", "Monsoon Rain"), 0, "Rongali Bihu welcomes spring! 🌸")
        )
        "complete_phrase" -> listOf(
            GameQuestion("Complete the proverb: 'Practice makes a person...'", listOf("Perfect", "Tired", "Fast"), 0, "Wisdom from timeless generations! ✨"),
            GameQuestion("Complete the saying: 'Early to bed and early to rise makes one...'", listOf("Healthy, wealthy and wise", "Sleepy in noon", "Busy all day"), 0, "A classic daily health anchor! ☀️"),
            GameQuestion("Complete the saying: 'Where there is a will, there is a...'", listOf("Way", "Road", "Home"), 0, "Determination always finds a path! 🌿")
        )
        "category_word" -> listOf(
            GameQuestion("Which of these belongs to the 'Kitchen Spices' family?", listOf("Turmeric (Haldi)", "Notebook", "Screwdriver"), 0, "Turmeric adds natural flavor & health! 🌿"),
            GameQuestion("Which of these is a fresh winter fruit?", listOf("Guava", "Plastic Cup", "Cotton"), 0, "Guavas are rich in vitamin C! 🍏"),
            GameQuestion("Which of these is used for writing?", listOf("Fountain Pen", "Spoon", "Umbrella"), 0, "Fountain pens capture memories! ✒️")
        )
        "day_time" -> listOf(
            GameQuestion("What time of the day comes right after afternoon?", listOf("Evening / Shaam", "Midnight", "Sunrise"), 0, "The gentle golden hour of evening! 🌅"),
            GameQuestion("Which day comes immediately after Saturday?", listOf("Sunday", "Friday", "Monday"), 0, "Sunday is a peaceful rest day! ☀️"),
            GameQuestion("In which season do flowers bloom and festivals arrive?", listOf("Spring (Basant)", "Harsh Winter", "Monsoon Flood"), 0, "Spring brings fresh green energy! 🌸")
        )
        "my_home" -> listOf(
            GameQuestion("Where do we keep the cooking pots and warm tea kettle?", listOf("In the Kitchen", "In the Bathroom", "In the Garden"), 0, "The kitchen is the heart of the home! 🫖"),
            GameQuestion("Where do you find your soft pillows and warm blanket?", listOf("Bedroom", "Verandah", "Kitchen"), 0, "Your peaceful resting space! 🛏️"),
            GameQuestion("Where do we welcome family guests who visit?", listOf("Living Room", "Store Room", "Roof"), 0, "The welcoming living room! 🛋️")
        )
        "find_your_way" -> listOf(
            GameQuestion("If you want to reach the front garden, you go through:", listOf("The Main Entrance Door", "The Cupboard", "The Bathroom"), 0, "The front door opens to the open garden! 🌿"),
            GameQuestion("To wash your hands before meal, which room do you visit?", listOf("Washbasin / Bathroom", "Balcony", "Garage"), 0, "Clean habits keep us healthy! 💧"),
            GameQuestion("When coming inside from rain, where do you keep the umbrella?", listOf("Umbrella stand near doorway", "On the bed", "In the fridge"), 0, "Near the doorway keeps floors dry! ☂️")
        )
        "object_sorting" -> listOf(
            GameQuestion("Sort into Wardrobe vs Kitchen: 'Woolen Sweater'", listOf("Wardrobe / Almari", "Kitchen Shelf", "Shoe Box"), 0, "Sweaters stay cozy in the wardrobe! 🧶"),
            GameQuestion("Sort into Kitchen vs Bathroom: 'Steel Plate & Bowl'", listOf("Kitchen Cabinet", "Soap Tray", "Bookshelf"), 0, "Plates and bowls belong in the kitchen! 🥣"),
            GameQuestion("Sort into Reading Table vs Medicine Box: 'Reading Glasses'", listOf("Bedside / Reading Table", "Spice Jar", "Laundry Basket"), 0, "Glasses stay ready beside your book! 👓")
        )
        "virtual_grocery" -> listOf(
            GameQuestion("Choose the ingredients for healthy morning vegetable soup:", listOf("Carrots, Spinach & Ginger", "Soap & Paint", "Thread & Needles"), 0, "Fresh vegetables nourish the body! 🥕"),
            GameQuestion("Which item is needed to make warm milk tea?", listOf("Tea leaves, Milk & Cardamom", "Chili Powder", "Salt & Vinegar"), 0, "A soothing, aromatic cup! ☕"),
            GameQuestion("Select the wholesome breakfast staple:", listOf("Oats & Bananas", "Battery Cells", "Paper Napkins"), 0, "Wholesome morning vitality! 🍌")
        )
        "emotion_match" -> listOf(
            GameQuestion("Identify the feeling: A smiling face with twinkling eyes:", listOf("Joyful & Happy 😊", "Angry", "Fearful"), 0, "A warm and radiant smile! 🌸"),
            GameQuestion("Identify the feeling: Taking a slow, deep breath in the garden:", listOf("Peaceful & Calm 🌿", "Rushed", "Scared"), 0, "Calmness centers our mind! ✨"),
            GameQuestion("Identify the feeling: Giving a loved grandchild a warm hug:", listOf("Loving & Caring ❤️", "Confused", "Cold"), 0, "Love and family affection! 👨‍👩‍👧")
        )
        else -> listOf(
            GameQuestion("Which color is the clear morning sky?", listOf("Sky Blue", "Coal Black", "Neon Green"), 0, "A beautiful clear sky! ☀️"),
            GameQuestion("How many days are there in a week?", listOf("7 Days", "12 Days", "4 Days"), 0, "Seven days of mindful living! 📅"),
            GameQuestion("What keeps our memory and body active?", listOf("Daily games, walks & good food", "Sitting idle", "Skipping sleep"), 0, "Stay active and joyful every day! 🌿")
        )
    }
}

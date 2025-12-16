package com.download.downloaderbot.bot.gateway.telegram.util

import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.MessageEntity
import com.github.kotlintelegrambot.entities.Update
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class CommandAddressingTest : FunSpec({

    val targetBotName = "my_bot"

    context("Update field priority") {
        test("uses message over editedMessage (no fallback)") {
            val msg = mockMessage(text = "hello")
            val edited = mockMessage(text = "/start", entities = listOf("/start".toEntity()))

            val update = mockUpdate(message = msg, editedMessage = edited)

            update.addressing(targetBotName) shouldBe CommandAddressing.NO_COMMAND
        }

        test("falls back to editedMessage when message is null") {
            val edited = mockMessage(text = "/start", entities = listOf("/start".toEntity()))
            val update = mockUpdate(message = null, editedMessage = edited)

            update.addressing(targetBotName) shouldBe CommandAddressing.OUR
        }

        test("returns NO_COMMAND when update is empty") {
            mockUpdate().addressing(targetBotName) shouldBe CommandAddressing.NO_COMMAND
        }
    }

    context("Command parsing and addressing logic") {

        data class TestCase(
            val name: String,
            val text: String? = null,
            val caption: String? = null,
            val entities: List<MessageEntity>? = null,
            val captionEntities: List<MessageEntity>? = null,
            val expected: CommandAddressing
        )

        val testCases = listOf(
            TestCase(
                name = "returns NO_COMMAND when entities present but text/caption null",
                entities = listOf("/start".toEntity()),
                expected = CommandAddressing.NO_COMMAND
            ),
            TestCase(
                name = "returns NO_COMMAND when first token does not start with '/'",
                text = "start",
                entities = listOf("start".toEntity()),
                expected = CommandAddressing.NO_COMMAND
            ),
            TestCase(
                name = "returns NO_COMMAND when BOT_COMMAND entity has negative offset",
                text = "/start",
                entities = listOf("/start".toEntity(offset = -1)),
                expected = CommandAddressing.NO_COMMAND
            ),
            TestCase(
                name = "returns OUR when command has no @mention",
                text = "/start hello",
                entities = listOf("/start".toEntity()),
                expected = CommandAddressing.OUR
            ),
            TestCase(
                name = "returns OUR when command mentions our username (case-insensitive)",
                text = "/start@My_Bot hello",
                entities = listOf("/start@My_Bot".toEntity()),
                expected = CommandAddressing.OUR
            ),
            TestCase(
                name = "returns OTHER when command mentions different username",
                text = "/start@other_bot hello",
                entities = listOf("/start@other_bot".toEntity()),
                expected = CommandAddressing.OTHER
            ),
            TestCase(
                name = "prefers entities over captionEntities (text is OTHER, caption is OUR)",
                text = "/start@other_bot",
                caption = "/start@my_bot",
                entities = listOf("/start@other_bot".toEntity()),
                captionEntities = listOf("/start@my_bot".toEntity()),
                expected = CommandAddressing.OTHER
            ),
            TestCase(
                name = "uses captionEntities when entities missing/invalid (fallback to caption)",
                caption = "/start@my_bot foo",
                entities = listOf(mockEntity(2, 6)),
                captionEntities = listOf("/start@my_bot".toEntity()),
                expected = CommandAddressing.OUR
            ),
            TestCase(
                name = "handles entity length exceeding text length (safe substring)",
                text = "/start",
                entities = listOf("/start".toEntity(length = 999)),
                expected = CommandAddressing.OUR
            )
        )

        testCases.forEach { testCase ->
            test(testCase.name) {
                val msg = mockMessage(
                    text = testCase.text,
                    caption = testCase.caption,
                    entities = testCase.entities,
                    captionEntities = testCase.captionEntities
                )
                val update = mockUpdate(message = msg)

                update.addressing(targetBotName) shouldBe testCase.expected
            }
        }
    }
})

private fun mockUpdate(
    message: Message? = null,
    editedMessage: Message? = null,
): Update = mockk {
    every { this@mockk.message } returns message
    every { this@mockk.editedMessage } returns editedMessage
    every { this@mockk.channelPost } returns null
    every { this@mockk.editedChannelPost } returns null
}

private fun mockMessage(
    text: String? = null,
    caption: String? = null,
    entities: List<MessageEntity>? = null,
    captionEntities: List<MessageEntity>? = null,
): Message = mockk {
    every { this@mockk.text } returns text
    every { this@mockk.caption } returns caption
    every { this@mockk.entities } returns entities
    every { this@mockk.captionEntities } returns captionEntities
}

private fun mockEntity(offset: Int, length: Int): MessageEntity = mockk {
    every { type } returns MessageEntity.Type.BOT_COMMAND
    every { this@mockk.offset } returns offset
    every { this@mockk.length } returns length
}

private fun String.toEntity(offset: Int = 0, length: Int = this.length): MessageEntity =
    mockEntity(offset, length)
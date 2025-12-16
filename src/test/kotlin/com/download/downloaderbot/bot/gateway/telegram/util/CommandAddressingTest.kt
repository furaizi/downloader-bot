package com.download.downloaderbot.bot.gateway.telegram.util

import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.MessageEntity
import com.github.kotlintelegrambot.entities.Update
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class CommandAddressingTest : FunSpec({

    context("Update.addressing") {

        test("returns NO_COMMAND when update has no message-like fields") {
            val update = mockUpdate()

            update.addressing("my_bot") shouldBe CommandAddressing.NO_COMMAND
        }

        test("uses message over editedMessage/channelPost/editedChannelPost (no fallback)") {
            val msgWithoutCommand = mockMessage(
                text = "hello",
                entities = emptyList(),
            )
            val editedWithCommand = mockMessage(
                text = "/start",
                entities = listOf(botCommandEntity(0, 6)),
            )

            val update = mockUpdate(
                message = msgWithoutCommand,
                editedMessage = editedWithCommand,
            )

            update.addressing("my_bot") shouldBe CommandAddressing.NO_COMMAND
        }

        test("falls back to editedMessage when message is null") {
            val editedWithCommand = mockMessage(
                text = "/start",
                entities = listOf(botCommandEntity(0, 6)),
            )
            val update = mockUpdate(editedMessage = editedWithCommand)

            update.addressing("my_bot") shouldBe CommandAddressing.OUR
        }

        test("returns NO_COMMAND when message has bot_command entity but text/caption are null") {
            val msg = mockMessage(
                entities = listOf(botCommandEntity(0, 6)),
            )
            val update = mockUpdate(message = msg)

            update.addressing("my_bot") shouldBe CommandAddressing.NO_COMMAND
        }

        test("returns NO_COMMAND when first token does not start with '/'") {
            val msg = mockMessage(
                text = "start",
                entities = listOf(botCommandEntity(0, 5)),
            )
            val update = mockUpdate(message = msg)

            update.addressing("my_bot") shouldBe CommandAddressing.NO_COMMAND
        }

        test("returns OUR when command has no @mention") {
            val msg = mockMessage(
                text = "/start hello",
                entities = listOf(botCommandEntity(0, 6)),
            )
            val update = mockUpdate(message = msg)

            update.addressing("my_bot") shouldBe CommandAddressing.OUR
        }

        test("returns OUR when command mentions our username (case-insensitive)") {
            val msg = mockMessage(
                text = "/start@My_Bot hello",
                entities = listOf(botCommandEntity(0, "/start@My_Bot".length)),
            )
            val update = mockUpdate(message = msg)

            update.addressing("my_bot") shouldBe CommandAddressing.OUR
        }

        test("returns OTHER when command mentions different username") {
            val msg = mockMessage(
                text = "/start@other_bot hello",
                entities = listOf(botCommandEntity(0, "/start@other_bot".length)),
            )
            val update = mockUpdate(message = msg)

            update.addressing("my_bot") shouldBe CommandAddressing.OTHER
        }

        test("prefers entities over captionEntities when both present") {
            val msg = mockMessage(
                text = "/start@other_bot",
                caption = "/start@my_bot",
                entities = listOf(botCommandEntity(0, "/start@other_bot".length)),
                captionEntities = listOf(botCommandEntity(0, "/start@my_bot".length)),
            )
            val update = mockUpdate(message = msg)

            update.addressing("my_bot") shouldBe CommandAddressing.OTHER
        }

        test("uses captionEntities when entities do not contain BOT_COMMAND at offset 0") {
            val msg = mockMessage(
                caption = "/start@my_bot foo",
                entities = listOf(
                    botCommandEntity(2, 6),
                ),
                captionEntities = listOf(
                    botCommandEntity(0, "/start@my_bot".length),
                ),
            )
            val update = mockUpdate(message = msg)

            update.addressing("my_bot") shouldBe CommandAddressing.OUR
        }

        test("returns NO_COMMAND when BOT_COMMAND entity has negative offset") {
            val msg = mockMessage(
                text = "/start",
                entities = listOf(botCommandEntity(-1, 6)),
            )
            val update = mockUpdate(message = msg)

            update.addressing("my_bot") shouldBe CommandAddressing.NO_COMMAND
        }

        test("truncates token when entity length exceeds source length (min())") {
            val msg = mockMessage(
                text = "/start",
                entities = listOf(botCommandEntity(0, 999)),
            )
            val update = mockUpdate(message = msg)

            update.addressing("my_bot") shouldBe CommandAddressing.OUR
        }
    }
})

private fun mockUpdate(
    message: Message? = null,
    editedMessage: Message? = null,
    channelPost: Message? = null,
    editedChannelPost: Message? = null,
): Update =
    mockk<Update>().also { u ->
        every { u.message } returns message
        every { u.editedMessage } returns editedMessage
        every { u.channelPost } returns channelPost
        every { u.editedChannelPost } returns editedChannelPost
    }

private fun mockMessage(
    text: String? = null,
    caption: String? = null,
    entities: List<MessageEntity>? = null,
    captionEntities: List<MessageEntity>? = null,
): Message =
    mockk<Message>().also { m ->
        every { m.text } returns text
        every { m.caption } returns caption
        every { m.entities } returns entities
        every { m.captionEntities } returns captionEntities
    }

private fun botCommandEntity(offset: Int, length: Int): MessageEntity =
    mockk<MessageEntity>().also { e ->
        every { e.type } returns MessageEntity.Type.BOT_COMMAND
        every { e.offset } returns offset
        every { e.length } returns length
    }

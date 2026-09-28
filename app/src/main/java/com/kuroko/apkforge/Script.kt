package com.kuroko.apkforge

data class Script(
    val name: String,
    val description: String,
    val author: String,
    val code: String
)

object ScriptRepository {

    val builtIn: List<Script> = listOf(
        Script(
            name = "print test",
            description = "confirms the executor is talking to the game",
            author = "kuroko",
            code = "print(\"[kuroko] hello from the void\")"
        ),
        Script(
            name = "walk speed",
            description = "sets LocalPlayer walkspeed to 64",
            author = "kuroko",
            code = """
                local plr = game:GetService("Players").LocalPlayer
                if plr and plr.Character and plr.Character:FindFirstChildOfClass("Humanoid") then
                    plr.Character:FindFirstChildOfClass("Humanoid").WalkSpeed = 64
                    print("[kuroko] walkspeed set")
                end
            """.trimIndent()
        ),
        Script(
            name = "jump power",
            description = "gives high jump",
            author = "kuroko",
            code = """
                local plr = game:GetService("Players").LocalPlayer
                if plr and plr.Character then
                    local h = plr.Character:FindFirstChildOfClass("Humanoid")
                    if h then h.UseJumpPower = true; h.JumpPower = 120 end
                end
            """.trimIndent()
        ),
        Script(
            name = "infinite jump",
            description = "jump while airborne",
            author = "kuroko",
            code = """
                local uis = game:GetService("UserInputService")
                local plr = game:GetService("Players").LocalPlayer
                uis.JumpRequest:Connect(function()
                    if plr.Character then
                        local h = plr.Character:FindFirstChildOfClass("Humanoid")
                        if h then h:ChangeState(Enum.HumanoidStateType.Jumping) end
                    end
                end)
            """.trimIndent()
        ),
        Script(
            name = "server hop",
            description = "teleports to a different server",
            author = "kuroko",
            code = """
                local ts = game:GetService("TeleportService")
                local plr = game:GetService("Players").LocalPlayer
                local place = game.PlaceId
                ts:Teleport(place, plr)
            """.trimIndent()
        ),
        Script(
            name = "rejoin",
            description = "rejoins current server",
            author = "kuroko",
            code = """
                game:GetService("TeleportService"):Teleport(game.PlaceId, game:GetService("Players").LocalPlayer)
            """.trimIndent()
        ),
        Script(
            name = "fps unlocker",
            description = "removes the 60fps cap",
            author = "kuroko",
            code = """
                local settings = UserSettings()
                settings.GameSettings.Rendering.QualityLevel = Enum.QualityLevel.Level01
                for _, v in pairs(game:GetDescendants()) do
                    if v:IsA("Decal") then v.Transparency = 1 end
                end
                print("[kuroko] fps tweak applied")
            """.trimIndent()
        ),
        Script(
            name = "anti afk",
            description = "stops you getting kicked for idling",
            author = "kuroko",
            code = """
                local vu = game:GetService("VirtualUser")
                game:GetService("Players").LocalPlayer.Idled:Connect(function()
                    vu:Button2Down(Vector2.new(0,0), workspace.CurrentCamera.CFrame)
                    task.wait(1)
                    vu:Button2Up(Vector2.new(0,0), workspace.CurrentCamera.CFrame)
                end)
                print("[kuroko] anti-afk attached")
            """.trimIndent()
        )
    )
}

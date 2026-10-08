window.addEventListener("load", () => {

    function chamarJavaComRetentativa(tentativas = 0) {
        if (window.javaConnector && typeof window.javaConnector.onAnimationEnd === "function") {
            console.log("Chamando window.javaConnector.onAnimationEnd()...");
            window.javaConnector.onAnimationEnd();
        } else if (tentativas < 10) {
            console.warn("javaConnector ainda não encontrado, tentando novamente em 200ms...");
            setTimeout(() => chamarJavaComRetentativa(tentativas + 1), 200);
        } else {
            console.error("ERRO CRÍTICO: window.javaConnector não foi injetado pelo JavaFX!");
        }
    }

    // Se o GSAP não for encontrado, aguarda 2,5s antes de tentar chamar o Java
    // Isso evita o disparo instantâneo e o fecho precoce da janela
    if (typeof gsap === "undefined") {
        console.error("GSAP não encontrado na WebView. Aguardando fallback...");
        setTimeout(() => chamarJavaComRetentativa(), 2500);
        return;
    }

    // Animação sincronizada com a ponte JavaFX
    const timeline = gsap.timeline({
        onComplete: () => {
            // Esconde completamente o conteúdo para evitar o deslocamento visual antes de trocar a tela
            document.body.style.display = "none";
            chamarJavaComRetentativa();
        }
    });

    timeline
        .fromTo("#logo",
            { opacity: 0, scale: 0.3, rotation: -15 },
            { opacity: 1, scale: 1, rotation: 0, duration: 1.2, ease: "back.out(1.5)" }
        )
        .fromTo("#title",
            { opacity: 0, y: 20 },
            { opacity: 1, y: 0, duration: 0.8, ease: "power2.out" },
            "-=0.4"
        )
        .fromTo("#subtitle",
            { opacity: 0, y: 10 },
            { opacity: 1, y: 0, duration: 0.6, ease: "power2.out" },
            "-=0.3"
        )
        .fromTo("#loader",
            { opacity: 0 },
            { opacity: 1, duration: 0.4 },
            "-=0.2"
        )
        .to(".loader-progress", {
            width: "100%",
            duration: 2.0,
            ease: "none"
        })
        .to({}, { duration: 0.2 })
        .to(".splash-container", {
            opacity: 0,
            duration: 0.3,
            ease: "power2.in"
        });
});
import { Nfc } from 'nfc';

window.testEcho = () => {
    const inputValue = document.getElementById("echoInput").value;
    Nfc.echo({ value: inputValue })
}

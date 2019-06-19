/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;

/*
 * @test
 * @bug 8050374
 * @key randomness
 * @summary This test validates signature verification
 *          Signature.verify(byte[], int, int). The test uses RandomFactory to
 *          get random set of clear text data to sign. After the signature
 *          generation, the test tries to verify signature with the above API
 *          and passing in different signature offset (0, 33, 66, 99).
 * @library /lib/testlibrary
 * @compile ../../../../java/security/Signature/Offsets.java
 * @run main SignatureOffsets SunJSSE MD2withRSA
 * @run main SignatureOffsets SunJSSE MD5withRSA
 * @run main SignatureOffsets SunJSSE SHA1withRSA
 * @run main SignatureOffsets SunJSSE MD5andSHA1withRSA
 */
public class SignatureOffsets {

    public static void main(String[] args) throws NoSuchAlgorithmException,
            InvalidKeyException, SignatureException {
        Offsets.main(args);
    }
}

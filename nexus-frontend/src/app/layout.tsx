import type { Metadata } from "next";
import { AuthProvider } from "@/context/AuthContext";
import "./globals.css";

export const metadata: Metadata = {
  title: "Nexus — AI-Powered Support Platform",
  description: "Intelligent ticket triage, instant knowledge retrieval, and enterprise-grade multi-tenant support operations. Powered by AI.",
  keywords: ["support", "AI", "ticket management", "knowledge base", "customer support"],
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body>
        {/* Aurora background — always present */}
        <div className="aurora" aria-hidden="true">
          <div className="aurora-blob aurora-blob--1" />
          <div className="aurora-blob aurora-blob--2" />
          <div className="aurora-blob aurora-blob--3" />
        </div>

        <AuthProvider>
          {children}
        </AuthProvider>
      </body>
    </html>
  );
}
